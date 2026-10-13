#!/usr/bin/env python3
"""Generate additive Java AG-UI 1.0 models from the vendored schema."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'camel-ag-ui-component/src/main'
DEFS = json.loads((BASE / 'resources/io/dscope/camel/agui/schema-1.0.json').read_text())['$defs']
MODEL = BASE / 'java/io/dscope/camel/agui/model'
PROTO = MODEL / 'protocol'
PROTO.mkdir(exist_ok=True)
HEADER = (MODEL / 'AgUiEvent.java').read_text().split('package ')[0]
def props(name):
    s=DEFS[name]; out={}
    for x in s.get('allOf',[]):
        if '$ref' in x: out.update(props(x['$ref'].split('/')[-1]))
    out.update(s.get('properties',{})); return out
unions={}
for name,s in DEFS.items():
    variants=[x['$ref'].split('/')[-1] for x in s.get('oneOf',[]) if '$ref' in x]
    if not variants or name=='Event':continue
    for key in ['type','role','op']:
        values=[props(v).get(key,{}).get('const') for v in variants]
        if all(x is not None for x in values) and len(set(values))==len(values):
            unions[name]=(key,list(zip(variants,values)));break
parents={}
for name,(key,variants) in unions.items():
    for v,_ in variants: parents[v]=name
    types=',\n    '.join('@JsonSubTypes.Type(value = '+v+'.class, name = "'+str(value)+'")' for v,value in variants)
    (PROTO/(name+'.java')).write_text(HEADER+f'''package io.dscope.camel.agui.model.protocol;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "{key}", visible = true)
@JsonSubTypes({{{types}}})
public interface {name} {{ }}
''')
def typ(s):
    if '$ref' in s:
        n=s['$ref'].split('/')[-1]
        if n in unions or DEFS[n].get('type')=='object' and not DEFS[n].get('additionalProperties',False): return n
        return typ(DEFS[n])
    t=s.get('type')
    if t=='string' or isinstance(s.get('const'),str): return 'String'
    if t=='integer':return 'Long'
    if t=='number':return 'Double'
    if t=='boolean':return 'Boolean'
    if t=='array':return 'List<'+typ(s.get('items',{}))+'>'
    if t=='object':return 'Map<String, Object>'
    return 'Object'
def body_fields(fields):
    out=[]
    for key,s in fields.items():
        t=typ(s);cap=key[0].upper()+key[1:];default=' = '+json.dumps(s['const']) if 'const' in s else ''
        if t == 'Object' and key in ['value','event'] and 'not' not in s: out.append('    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)')
        out.append(f'    private {t} {key}{default};\n    public {t} get{cap}() {{ return {key}; }}\n    public void set{cap}({t} value) {{ this.{key} = value; }}')
    return '\n'.join(out)
open_members = """    private final Map<String,Object> extensions = new java.util.LinkedHashMap<>();
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void setExtension(String name,Object value) { extensions.put(name,value); }
    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public Map<String,Object> getExtensions() { return extensions; }"""
for name,s in DEFS.items():
    if s.get('type')!='object' or name.endswith('Event') or name in ['BaseEvent','Attributable','BaseMessage'] or s.get('additionalProperties',False):continue
    fields=props(name);implements=' implements '+parents[name] if name in parents else ''
    (PROTO/(name+'.java')).write_text(HEADER+f'''package io.dscope.camel.agui.model.protocol;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class {name}{implements} {{
{body_fields(fields)}
{open_members if name.endswith('Operation') else ''}
}}
''')
for ref in DEFS['Event']['oneOf']:
    name=ref['$ref'].split('/')[-1];short=name.removesuffix('Event');java='AgUi'+short
    if (MODEL/(java+'.java')).exists():continue
    fields=props(name);type_=fields['type']['const']
    for key in ['type','timestamp','metadata','rawEvent']:fields.pop(key,None)
    # All attribution is exposed by the base model with an explicit schema check.
    fields.pop('subagentRunId',None)
    (MODEL/(java+'.java')).write_text(HEADER+f'''package io.dscope.camel.agui.model;
import java.util.List;
import java.util.Map;
import io.dscope.camel.agui.model.protocol.*;
public class {java} extends AgUiEvent {{
    public {java}() {{ super("{type_}", null, null); }}
    public {java}(String runId, String sessionId) {{ super("{type_}", runId, sessionId); }}
{body_fields(fields)}
}}
''')
p=MODEL/'AgUiEventTypes.java';code=p.read_text()
for value in DEFS['EventType']['enum']:
    if 'String '+value+' =' not in code:code=code.replace('    private AgUiEventTypes()',f'    public static final String {value} = "{value}";\n\n    private AgUiEventTypes()')
p.write_text(code)
