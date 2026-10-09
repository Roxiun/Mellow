#!/usr/bin/env python3
"""Regenerate MCP development names over Calamus gen2; run from the repository root."""
import zipfile,csv,io,re
from pathlib import Path
import subprocess
root=Path('build/mapping-inputs')
root.mkdir(parents=True, exist_ok=True)
inputs = {
    'mcp-srg.zip': 'https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.8.9/mcp-1.8.9-srg.zip',
    'mcp-names.zip': 'https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/22-1.8.9/mcp_stable-22-1.8.9.zip',
    'calamus.jar': 'https://maven.ornithemc.net/releases/net/ornithemc/calamus-intermediary-gen2/1.8.9/calamus-intermediary-gen2-1.8.9-v2.jar',
}
for filename, url in inputs.items():
    if not (root/filename).is_file():
        subprocess.run(['curl', '--fail', '--location', url, '--output', str(root/filename)], check=True)
srg=zipfile.ZipFile(root/'mcp-srg.zip').read('joined.srg').decode().splitlines()
names={}
z=zipfile.ZipFile(root/'mcp-names.zip')
for f in ['fields.csv','methods.csv']:
 names.update({v['searge']:v['name'] for v in csv.DictReader(io.StringIO(z.read(f).decode()))})
classes={};fields={};methods={}
for l in srg:
 v=l.split()
 if v[0]=='CL:':classes[v[1]]=v[2]
 elif v[0]=='FD:':fields[v[1]]=names.get(v[2].rsplit('/',1)[1],v[2].rsplit('/',1)[1])
 elif v[0]=='MD:':methods[(v[1],v[2])]=names.get(v[3].rsplit('/',1)[1],v[3].rsplit('/',1)[1])
tiny=zipfile.ZipFile(root/'calamus.jar').read('mappings/mappings.tiny').decode().splitlines()
inter={v.split('\t')[1]:v.split('\t')[2] for v in tiny if v.startswith('c\t')}
out=['tiny\t2\t0\tintermediary\tnamed']
for l in tiny[1:]:
 v=l.split('\t')
 if v[0]=='c':
  owner=v[1];out.append('c\t'+v[2]+'\t'+classes.get(owner,v[2]))
 elif len(v)>4 and v[1] in ['f','m']:
  desc=re.sub(r'L([^;]+);',lambda m:'L'+inter.get(m[1],m[1])+';',v[2])
  name=fields.get(owner+'/'+v[3],v[4]) if v[1]=='f' else methods.get((owner+'/'+v[3],v[2]),v[4])
  if v[3].startswith('<'):name=v[3]
  out.append('\t'+v[1]+'\t'+desc+'\t'+v[4]+'\t'+name)
# Calamus shares method identifiers along inheritance chains. Propagate MCP names
# for matching descriptors so interface/implementation names remain consistent.
method_names = {}
for line in out:
    parts = line.split('\t')
    if len(parts) == 5 and parts[1] == 'm' and parts[3] != parts[4]:
        method_names[parts[3], parts[2]] = parts[4]
for i, line in enumerate(out):
    parts = line.split('\t')
    if len(parts) == 5 and parts[1] == 'm':
        parts[4] = method_names.get((parts[3], parts[2]), parts[4])
        out[i] = '\t'.join(parts)
Path('mappings/mcp-1.8.9.tiny').write_text('\n'.join(out)+'\n')

# Record canonical names separately: on-disk replays must survive loader mapping changes.
packet_types = ['# Canonical Minecraft 1.8.9 play/clientbound names -> Calamus gen2']
for line in out:
    parts = line.split('\t')
    if parts[0] == 'c' and parts[2].startswith('net/minecraft/network/play/server/'):
        packet_types.append(parts[2].replace('/', '.') + '=' + parts[1].replace('/', '.'))
Path('src/ornithe/main/resources/mellow-packet-types.properties').write_text('\n'.join(packet_types) + '\n')
