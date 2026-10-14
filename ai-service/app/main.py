"""Read-only inference service: Spring Boot is the only client."""
from __future__ import annotations
import os, secrets
from fastapi import FastAPI, Header, HTTPException, status
from pydantic import BaseModel, ConfigDict, Field, model_validator
from .scoring import InputRow, analyze

app=FastAPI(title='ReconPilot Analytics',version='0.2.0',docs_url='/docs')

class Candidate(BaseModel):
    model_config=ConfigDict(extra='forbid',str_strip_whitespace=True)
    transaction_id: str=Field(min_length=1,max_length=100)
    amount_cents: int=Field(ge=-10**12,le=10**12)
    delta_cents: int=Field(ge=-10**12,le=10**12)
    date_gap_days: int=Field(ge=0,le=365)
    possible_duplicate: bool=False
    reference_conflict: bool=False

class AnalyzeRequest(BaseModel):
    model_config=ConfigDict(extra='forbid')
    batch_id: str=Field(min_length=1,max_length=100)
    candidates: list[Candidate]=Field(min_length=1,max_length=500)
    enable_iforest: bool=False
    @model_validator(mode='after')
    def unique(self):
        ids=[v.transaction_id for v in self.candidates]
        if len(ids)!=len(set(ids)):raise ValueError('Duplicate transaction_id in request')
        return self

@app.get('/healthz')
def healthz():
    return {'status':'ok','service':'reconpilot-analytics','version':'0.2.0'}

@app.post('/v1/anomalies/analyze')
def analyze_endpoint(request:AnalyzeRequest,x_internal_token:str|None=Header(default=None)):
    expected=os.getenv('AI_INTERNAL_TOKEN','')
    # Fail-closed: no secret configured => service unavailable, not open access.
    if not expected:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail='AI_INTERNAL_TOKEN is not configured')
    if not x_internal_token or not secrets.compare_digest(expected,x_internal_token):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED,detail='Unauthorized')
    rows=[InputRow(**v.model_dump(exclude_none=True)) for v in request.candidates]
    items,method=analyze(rows,enable_iforest=request.enable_iforest)
    return {'batch_id':request.batch_id,'model_version':'analytics-0.2.0',
            'method':method,'decision_authority':'none_advisory_only',
            'scores_are_probabilities':False,'items':items}
