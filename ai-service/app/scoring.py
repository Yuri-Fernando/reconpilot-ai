"""Advisory batch anomaly scoring; never determines financial reconciliation.

Scores are bounded [0, 1] and are NOT calibrated probabilities of fraud.
The optional Isolation Forest is unsupervised and trained per batch, not a
persisted production model. Only synthetic data should be used for this POC.
"""
from __future__ import annotations
from dataclasses import dataclass
from math import log1p
from statistics import median

@dataclass(frozen=True)
class InputRow:
    transaction_id: str
    amount_cents: int
    delta_cents: int
    date_gap_days: int
    possible_duplicate: bool
    reference_conflict: bool


def _mad(values: list[float]) -> tuple[float, float]:
    center = median(values)
    return center, median([abs(v - center) for v in values])


def analyze(rows: list[InputRow], enable_iforest: bool = False) -> tuple[list[dict], str]:
    """Return advisory classifications and optional IsolationForest signal.

    Deterministic base score and explanations; optional IF adds an indicator
    but never changes source transactions or reconciliation decisions.
    """
    if not rows: return [], 'heuristic_v1'
    amounts = [log1p(abs(r.amount_cents)) for r in rows]
    center, mad = _mad(amounts)
    extra = [0.0] * len(rows)
    method = 'heuristic_v1'
    if enable_iforest and len(rows) >= 20:
        try:
            import numpy as np
            from sklearn.ensemble import IsolationForest
            matrix = np.array([[log1p(abs(r.amount_cents)),
                                log1p(abs(r.delta_cents)), r.date_gap_days,
                                int(r.possible_duplicate), int(r.reference_conflict)]
                               for r in rows], dtype=float)
            model = IsolationForest(n_estimators=100, contamination='auto', random_state=42)
            anomaly_flags = model.fit_predict(matrix)
            extra = [0.10 if flag == -1 else 0.0 for flag in anomaly_flags]
            method = 'heuristic_plus_iforest_v1'
        except ImportError:
            method = 'heuristic_v1_dependency_unavailable'
    output=[]
    for i,r in enumerate(rows):
        reasons=[]
        score=0.0
        if r.reference_conflict:
            score+=0.38; reasons.append('reference_conflict')
        if r.possible_duplicate:
            score+=0.35; reasons.append('possible_duplicate')
        if abs(r.delta_cents)>0:
            score+=min(0.30,0.08+0.22*min(abs(r.delta_cents)/max(abs(r.amount_cents),1),1))
            reasons.append('amount_difference')
        if r.date_gap_days>2:
            score+=min(0.17,0.05+0.03*(r.date_gap_days-2))
            reasons.append('date_outside_tolerance')
        # Robust scale is descriptive; capped influence and no floating overflow.
        rz=0.6745*abs(amounts[i]-center)/(mad if mad > 1e-6 else 1.0)
        if len(rows)>=5 and rz>3.5:
            score+=0.10; reasons.append('amount_outlier')
        if extra[i] > 0:
            score += extra[i]; reasons.append('isolation_forest_outlier')
        score=round(min(1.0,max(0.0,score)),4)
        severity='high' if score >= 0.65 else 'medium' if score >= 0.30 else 'low'
        output.append({'transaction_id':r.transaction_id,'risk_score':score,
                       'severity':severity,'reasons':reasons,
                       'requires_human_review':bool(reasons)})
    return output,method
