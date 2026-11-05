import math, json, os
R=range
def frame(N):
    return lambda x,y,z: sum(c in (0,N-1) for c in (x,y,z))>=2
I={}
def d(n,N,f,cav=True): I[n]=(N,f,cav)
C,O='c','o'
b=lambda v: C if v else None
d(1,4,lambda x,y,z:b(not(x>=1 and y>=1 and z>=1)))
d(2,4,lambda x,y,z:b(frame(4)(x,y,z)))
d(3,7,lambda x,y,z:C if z%2==0 else (O if x<=5 and y<=5 else None),False)
d(4,4,lambda x,y,z:b(not(y>=1 and 1<=z<=2)))
d(5,4,lambda x,y,z:b(not(x>=2 and y>=2 and z>=2)))
d(6,5,lambda x,y,z:b(not((y==4 and z in(1,3) and x>=1) or (z==4 and x in(1,3) and y>=1))))
d(7,5,lambda x,y,z:C if frame(5)(x,y,z) else (O if 1<=x<=3 and 1<=y<=3 and 1<=z<=3 else None),False)
d(8,5,lambda x,y,z:b(not((x==4 and y in(1,3) and z in(1,3)) or (z==4 and x in(1,3) and y in(1,3)) or (y==4 and x in(1,3) and z in(1,3)))))
d(9,4,lambda x,y,z:b((z>=2 and x<=1) or (z<=1 and x>=2) or (z in(1,2) and x in(1,2))))
G=["XXXXX","X...X","X.X.X","X.XXX","XXXXX"]
d(10,5,lambda x,y,z:b(z<=2 or G[y][x]=='X'))
d(11,5,lambda x,y,z:C if (x in(0,1,3,4) and y in(0,1,3,4) and (x in(0,4) or y in(0,4))) and (x in(0,1)or x in(3,4)) and (y in(0,1) or y in(3,4)) and not(x in(1,3) and y in(1,3)) else (O if (x==2 or y==2) and z==2 else None),False)
d(12,5,lambda x,y,z:(O if (x,y,z)==(4,4,4) or (x+y+z)%4==0 and x==4 else C) if x%2==0 and y%2==0 and z%2==0 else None,False)
d(13,6,lambda x,y,z:C if z<=2 or z>=4 else (O if x<=4 and y<=4 else None),False)
d(14,7,lambda x,y,z:b(not((x>=5 and y in(1,3,5)) )))
d(15,4,lambda x,y,z:b(not(1<=z<=2 and x>=2)))
d(16,7,lambda x,y,z:(O if ((x+y+z)//2)%2 else C) if x%2==0 and y%2==0 and z%2==0 else None,False)
d(17,5,lambda x,y,z:O if 2<=x<=3 and 1<=z<=3 and 1<=y<=3 else b(not(x>=1 and 1<=z<=3)))
d(18,5,lambda x,y,z:b(sum(1<=c<=3 for c in(x,y,z))<=1))
M=["X...X","XX.XX","X.X.X","X...X","X...X"]
d(19,5,lambda x,y,z:C if M[4-z][x]=='X' else (O if z==0 and y>=3 else None))
d(20,5,lambda x,y,z:b(z<=4-max(x,y)+0 or z==0))
d(21,6,lambda x,y,z:O if 3<=x<=4 and 3<=y<=4 and z<=1 and z>=1 else b(not(x>=1 and y>=1 and z>=1 and x+y+z>=6)))
d(22,4,lambda x,y,z:b(not(x>=2 and z>=2)))
d(23,5,lambda x,y,z:O if 2<=x<=3 and 2<=y<=3 and 2<=z<=3 else b(not(x>=1 and y>=1 and z>=1)))
d(24,5,lambda x,y,z:b(not((z==4 and x in(1,3) and y in(1,3)) or (x==4 and y in(1,3) and z in(1,3)) or (y==4 and x in(1,3) and z in(1,3)))))
d(25,5,lambda x,y,z:O if (x,y,z) in [(2,2,1),(2,2,2),(3,2,3),(2,3,3),(3,3,3),(1,2,3),(2,1,3)] else b(not(x>=1 and y>=1 and z>=1)))
d(26,6,lambda x,y,z:C if z<=1 or z>=4 else (O if 1<=x<=4 and 1<=y<=4 else None),False)
d(27,4,lambda x,y,z:O if x>=2 and y>=2 and z>=2 else C,False)
H=[[4,3,4,2],[3,4,2,3],[4,2,3,1],[2,3,1,2]]
d(28,4,lambda x,y,z:b(z<H[x][y]))
d(29,5,lambda x,y,z:b(not(1<=x<=3 and 1<=y<=3 and z>=1)))
d(30,5,lambda x,y,z:b((z>=3 and x<=2 and y>=2) or (z<=1 and x>=2 and y<=2) or (z==2 and x==2) or (z>=3 and y==2 and x<=2)))
d(31,5,lambda x,y,z:O if x>=3 and y>=3 and z>=3 else b(frame(5)(x,y,z)))
d(32,5,lambda x,y,z:(O if 0 in(x,y,z) or 4 in(x,y,z) else C) if sum(c==2 for c in(x,y,z))>=2 else None,False)
d(33,5,lambda x,y,z:C if x<=3 and y<=3 and z<=3 else (O if x==4 and y<=3 and z<=3 and (y in(1,2) or z in(1,2)) else (O if z==4 and x<=3 and y<=3 and (x in(1,2) or y in(1,2)) else None)),False)
d(34,7,lambda x,y,z:(O if x==6 else C) if z%2==0 and x>=0 else None,False)
d(35,5,lambda x,y,z:b(not(x>=3 and 1<=y<=3)))
d(36,6,lambda x,y,z:b(not(x>=1 and y>=1 and z>=1) or (1<=x<=4 and 1<=y<=4 and 1<=z<=4 and not(x>=3 and y>=3 and z>=3))))
d(37,7,lambda x,y,z:(O if z==6 else C) if x%2==0 and y%2==0 else None,False)
d(38,5,lambda x,y,z:C if 1<=x<=3 and 1<=y<=3 and 1<=z<=3 else (O if frame(5)(x,y,z) else None),False)
def xs(x,y,z):
    a=[(x==y==z),(x==y==4-z),(x==4-y==z),(4-x==y==z)]; return any(a)
d(39,5,lambda x,y,z:((O if 0 in(x,y,z) and 4 in(x,y,z) or (x,y,z) in[(0,0,0),(4,4,4)] else C) if xs(x,y,z) else (C if (x,y,z)==(2,2,2) else None)),False)
d(40,5,lambda x,y,z:(O if (x+y)%4==2 else C) if x%2==0 and y%2==0 else None,False)
d(41,5,lambda x,y,z:C if (x==0 or y==0 or z==0) else (O if 2<=x<=3 and 2<=y<=3 and 1<=z<=2 else None),False)
d(42,5,lambda x,y,z:O if 1<=z<=3 and (x==4 or y==4) else C,False)
# --- Teamily AI brand palette ---------------------------------------------
# Every value below is a literal from app/src/main/java/ai/orhan/design/theme/Color.kt.
# Nothing is invented. Face order matches the original pack: t = top face
# (lightest, key light from above), l = left face (mid), r = right face
# (darkest, in shadow). Keeping that order preserves the voxel read.
#
#   c = structural body  : charcoal ramp  -> green-tinted neutral ramp
#   o = accent / cavity  : orange ramp    -> mint -> green ramp
COL={
    ('c','t'):'#4F6157',   # Ink600        — lightest body
    ('c','l'):'#235039',   # DarkPrimarySoft — mid body
    ('c','r'):'#182B23',   # Ink900        — darkest body
    ('o','t'):'#42D982',   # Mint          — lightest accent
    ('o','l'):'#0B8249',   # Green750      — mid accent
    ('o','r'):'#066A3B',   # Green800      — darkest accent
}
out={}
for n,(N,f,cav) in I.items():
    k=22/(2*N); c30=math.cos(math.pi/6)
    P=lambda x,y,z:(12+(x-y)*k*c30,12+(x+y)*k*.5-z*k)
    g=lambda x,y,z: f(x,y,z) if all(0<=c<N for c in(x,y,z)) else None
    paths={kk:[] for kk in COL}
    vox=sorted([(x,y,z) for x in R(N) for y in R(N) for z in R(N) if g(x,y,z)],key=lambda v:(sum(v),v[2]))
    for (x,y,z) in vox:
        col=g(x,y,z)
        for face,nb,pts in [('t',(x,y,z+1),[(x,y,z+1),(x+1,y,z+1),(x+1,y+1,z+1),(x,y+1,z+1)]),
                            ('r',(x+1,y,z),[(x+1,y,z),(x+1,y+1,z),(x+1,y+1,z+1),(x+1,y,z+1)]),
                            ('l',(x,y+1,z),[(x,y+1,z),(x+1,y+1,z),(x+1,y+1,z+1),(x,y+1,z+1)])]:
            if g(*nb): continue
            inside=all(0<=c<N for c in nb)
            fc='o' if col=='o' or (cav and inside) else 'c'
            paths[(fc,face)].append((sum(nb),'M'+'L'.join('%.2f %.2f'%P(*p) for p in pts)+'Z'))
    # painter order needs interleaving: emit in global order, grouping consecutive same-color
    allf=[]
    for kk,v in paths.items():
        for s,dd in v: allf.append((s,kk,dd))
    allf.sort(key=lambda t:t[0])
    segs=[]
    for s,kk,dd in allf:
        if segs and segs[-1][0]==kk: segs[-1][1].append(dd)
        else: segs.append([kk,[dd]])
    out[n]=[(COL[kk],''.join(ds)) for kk,ds in segs]
    svg='<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'+''.join('<path fill="%s" d="%s"/>'%p for p in out[n])+'</svg>'
    open('svg/cube_%02d.svg'%n,'w').write(svg)
    xml='<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'+''.join('  <path android:fillColor="%s" android:pathData="%s"/>\n'%p for p in out[n])+'</vector>\n'
    open('drawable/ic_cube_%02d.xml'%n,'w').write(xml)
kt=['package com.example.icons.cube','','import androidx.compose.ui.graphics.Color','import androidx.compose.ui.graphics.SolidColor','import androidx.compose.ui.graphics.vector.ImageVector','import androidx.compose.ui.graphics.vector.addPathNodes','import androidx.compose.ui.unit.dp','',
'/** 42 isometric charcoal/orange cube icons. Usage: Icon(CubeIcons.Cube01, null, tint = Color.Unspecified) */',
'object CubeIcons {',
'    private fun build(name: String, vararg paths: Pair<Long, String>): ImageVector =',
'        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {',
'            paths.forEach { (c, d) -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color(c))) }',
'        }.build()','']
for n,ps in out.items():
    kt.append('    val Cube%02d: ImageVector by lazy { build("Cube%02d",'%(n,n))
    kt+=['        0xFF%sL to "%s",'%(c[1:],dd) for c,dd in ps]
    kt.append('    ) }')
kt.append('\n    val all: List<ImageVector> by lazy { listOf(%s) }\n}'%', '.join('Cube%02d'%n for n in out))
open('compose/CubeIcons.kt','w').write('\n'.join(kt)+'\n')
json.dump({n:'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">'+''.join('<path fill="%s" d="%s"/>'%p for p in ps)+'</svg>' for n,ps in out.items()},open('svgs.json','w'))
