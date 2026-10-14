from fastapi.testclient import TestClient
from app.main import app
from app.scoring import InputRow,analyze

client=TestClient(app)

def case(id='a',**kw):
    d=dict(transaction_id=id,amount_cents=10000,delta_cents=0,date_gap_days=0,
           possible_duplicate=False,reference_conflict=False)
    d.update(kw);return d

def test_health():
    assert client.get('/healthz').json()['status']=='ok'

def test_fails_closed_if_token_missing(monkeypatch):
    monkeypatch.delenv('AI_INTERNAL_TOKEN',raising=False)
    assert client.post('/v1/anomalies/analyze',json={'batch_id':'b','candidates':[case()]}).status_code==503

def test_rejects_invalid_token(monkeypatch):
    monkeypatch.setenv('AI_INTERNAL_TOKEN','secret')
    r=client.post('/v1/anomalies/analyze',headers={'X-Internal-Token':'wrong'},json={'batch_id':'b','candidates':[case()]})
    assert r.status_code==401

def test_safe_default_no_auto_decision(monkeypatch):
    monkeypatch.setenv('AI_INTERNAL_TOKEN','secret')
    r=client.post('/v1/anomalies/analyze',headers={'X-Internal-Token':'secret'},json={'batch_id':'b','candidates':[case()]})
    assert r.status_code==200
    data=r.json()
    assert data['decision_authority']=='none_advisory_only'
    assert data['scores_are_probabilities'] is False
    assert data['items'][0]['risk_score']==0
    assert data['items'][0]['requires_human_review'] is False

def test_score_explains_multiple_warnings():
    a,_=analyze([InputRow(**case(possible_duplicate=True,reference_conflict=True,delta_cents=4000,date_gap_days=9))])
    assert a[0]['severity']=='high'
    assert {'possible_duplicate','reference_conflict','amount_difference','date_outside_tolerance'} <= set(a[0]['reasons'])

def test_determinism():
    rows=[InputRow(**case(str(i),delta_cents=i*100)) for i in range(25)]
    assert analyze(rows,enable_iforest=True)==analyze(rows,enable_iforest=True)

def test_duplicate_id_422(monkeypatch):
    monkeypatch.setenv('AI_INTERNAL_TOKEN','secret')
    r=client.post('/v1/anomalies/analyze',headers={'X-Internal-Token':'secret'},json={'batch_id':'b','candidates':[case(),case()]})
    assert r.status_code==422

def test_input_validation(monkeypatch):
    monkeypatch.setenv('AI_INTERNAL_TOKEN','secret')
    r=client.post('/v1/anomalies/analyze',headers={'X-Internal-Token':'secret'},json={'batch_id':'b','candidates':[case(date_gap_days=-9)]})
    assert r.status_code==422

def test_score_bounds():
    rows=[InputRow(**case(str(i),delta_cents=10**9,date_gap_days=365,possible_duplicate=True,reference_conflict=True)) for i in range(20)]
    a,method=analyze(rows,enable_iforest=True)
    assert all(0<=x['risk_score']<=1 for x in a)
    assert method in ('heuristic_plus_iforest_v1','heuristic_v1_dependency_unavailable')
