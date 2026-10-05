# MiniJaja / JajaCode — fiche de référence

Synthèse des deux documents de cours (F. Bouquet, Université de Franche-Comté), réorganisée comme spécification de travail pour l'implémentation.

---

## 1. Vue d'ensemble de la chaîne

```
Texte MiniJaja
      │
      ▼
  Analyseur lexical ──────► liste de lexèmes
      │
      ▼
  Analyseur syntaxique ───► AST MiniJaja (S.A. MiniJaja)
      │
      ├──────────────► Contrôleur de type ──► Erreurs
      │
      ├──► Interpréteur MiniJaja ─────────────┐
      │                                        ├──► Résultat
      └──► Compilateur ──► AST JajaCode ──► Interpréteur JajaCode ──┘
                                              (VM : Pile + Tas)
```

**Deux chemins d'exécution** pour le même programme, et c'est tout l'intérêt du sujet : on peut prouver leur équivalence sémantique (voir §9).

**Quatre livrables** possibles : analyseur (lexer + parser), interpréteur MiniJaja, compilateur vers JajaCode, interpréteur JajaCode.

---

## 2. Syntaxe concrète MiniJaja

Grammaire BNF. Colonne de gauche : règle. Colonne de droite : constructeur d'AST associé.

```
classe    → class ident "{" decls methmain "}"      classe($2,$4,$5)
ident     → identificateur                          ident($1)
decls     → decl ";" decls | vide                   decls($1,$3) | vnil
decl      → var | methode                           $1
vars      → var ";" vars | vide                     vars($1,$3) | vnil
var       → typemeth ident vexp                     var($1,$2,$3)
          | typemeth ident "[" exp "]"              tableau($1,$2,$4)
          | final type ident vexp                   cst($2,$3,$4)
vexp      → "=" exp | vide                          $2 | omega
methode   → typemeth ident "(" entêtes ")"
            "{" vars instrs "}"                     méthode($1,$2,$4,$7,$8)
methmain  → main "{" vars instrs "}"                main($3,$4)
entêtes   → entête "," entêtes                      entêtes($1,$3)
          | entête | vide                           entêtes($1,enil) | enil
entête    → type ident                              entête($1,$2)
instrs    → instr ";" instrs | vide                 instrs($1,$3) | inil
instr     → ident1 "=" exp                          affectation($1,$3)
          | ident1 "+=" exp                         somme($1,$3)
          | ident1 "++"                             incrément($1)
          | ident "(" listexp ")"                   appelI($1,$3)
          | return exp                              retour($2)
          | if exp "{" instrs "}" [else "{" instrs "}"]   si($2,$4,$8)
          | while exp "{" instrs "}"                tantque($2,$4)
listexp   → exp "," listexp                         listexp($1,$3)
          | exp | vide                              listexp($1,exnil) | exnil
exp       → "!" exp1 | "-" exp1                     non($2) | moins($2)
          | exp "&&" exp1                           et($1,$3)
          | exp "||" exp1 | exp1                    ou($1,$3) | $1
exp1      → exp1 "==" exp2                          =($1,$3)
          | exp1 ">" exp2 | exp2                    >($1,$3) | $1
exp2      → exp2 "+" terme                          +($1,$3)
          | exp2 "-" terme | terme                  -($1,$3) | $1
terme     → terme "*" fact                          *($1,$3)
          | terme "/" fact | fact                   /($1,$3) | $1
fact      → ident1                                  $1
          | ident "(" listexp ")"                   appelE($1,$3)
          | true | false | nombre                   vrai | faux | nbre($1)
          | "(" exp ")"                             $2
ident1    → ident | ident "[" exp "]"               $1 | tab($1,$3)
typemeth  → void | type                             rien | $1
type      → int | boolean                           entier | booléen
```

### Lexèmes

```
MOTS-CLÉS
  class   final   main    void    int     boolean
  if      else    while   return  true    false

OPÉRATEURS
  =   +=  ++                  affectation
  +   -   *   /               arithmétique   (- aussi unaire)
  ==  >                       comparaison
  &&  ||  !                   logique

DÉLIMITEURS
  (  )    [  ]    {  }    ;    ,
```

- **Mots-clés** : les 12 mots réservés de la grammaire. Le lexer doit les reconnaître avant `identificateur`. Les mots `write`, `writeln` et `length` du piège 6 n'y sont pas encore.
- **Opérateurs** : le lexer applique la règle du plus long préfixe (`++` et `+=` avant `+`, `==` avant `=`). Les opérateurs `&&` et `||` n'ont pas de forme à un caractère.

### Précédence des opérateurs (du plus faible au plus fort)

| Niveau | Opérateurs | Associativité |
|---|---|---|
| `exp` | `&&`, `\|\|`, et unaires `!`, `-` | gauche |
| `exp1` | `==`, `>` | gauche |
| `exp2` | `+`, `-` | gauche |
| `terme` | `*`, `/` | gauche |
| `fact` | littéraux, appels, parenthèses | — |

### ⚠ Pièges de cette grammaire

1. **Récursivité à gauche** sur `exp`, `exp1`, `exp2`, `terme`. En descente récursive, transformez chaque niveau en boucle :
   ```java
   Exp parseExp2() {
       Exp g = parseTerme();
       while (peek() == PLUS || peek() == MOINS) {
           Token op = next();
           Exp d = parseTerme();
           g = new BinOp(op, g, d);   // associativité gauche
       }
       return g;
   }
   ```
2. **`&&` et `||` sont au même niveau**, contrairement à Java. Suivez la spec, documentez l'écart.
3. **Les unaires `!` et `-` sont au niveau le plus faible**, donc `-a * b` se parse comme `-(a * b)`. Inhabituel — à signaler dans le README.
4. **`identificateur` et `nombre` ne sont pas définis.** À vous de fixer les conventions lexicales (forme des identifiants, entiers signés ou non, commentaires, blancs).
5. **Dangling else** : `if` avec `else` optionnel. Règle standard : le `else` se rattache au `if` le plus proche. Ici les accolades sont obligatoires, donc le problème disparaît.
6. **`ecrire`, `ecrireln`, `longueur`, `chaine` existent en syntaxe abstraite mais pas dans la grammaire fournie.** Il manque les règles concrètes correspondantes — à ajouter vous-même (`write`, `writeln`, `.length`, littéraux chaîne).

---

## 3. Syntaxe abstraite MiniJaja

Signature des constructeurs. C'est directement votre hiérarchie de classes Java.

### Structure du programme
```
classe   : ID × DECLS × MAIN → CLASSE
ident    : string → ID
main     : DECLS × INSTRS → MAIN
decls    : DECL × DECLS → DECLS
vars     : DECL × DECLS → DECLS
vnil     : → DECLS
```

### Déclarations
```
var      : TYPE × ID × EXP → DECL
cst      : TYPE × ID × EXP → DECL
tableau  : TYPE × ID × EXP → DECL
méthode  : TYPEMETH × ID × ENTETES × DECLS × INSTRS → DECL
entêtes  : ENTETE × ENTETES → ENTETES
entête   : TYPE × ID → ENTETE
enil     : → ENTETES
```

### Types
```
rien, entier, booléen : → TYPEMETH
entier, booléen       : → TYPE
```

### Instructions
```
instrs      : INSTR × INSTRS → INSTRS
inil        : → INSTRS
affectation : ID × EXP → INSTR
somme       : ID × EXP → INSTR
incrément   : ID → INSTR
appelI      : ID × LISTEXP → INSTR
retour      : EXP → INSTR
ecrire      : ID → INSTR
ecrireln    : ID → INSTR
si          : EXP × INSTRS × INSTRS → INSTR
tantque     : EXP × INSTRS → INSTR
```

### Expressions
```
listexp  : EXP × LISTEXP → LISTEXP
exnil    : → LISTEXP
appelE   : ID × LISTEXP → EXP'
tab      : ID × EXP → ID
longueur : ID → EXP'
non, moins       : EXP → EXP'
ou, et, =, >     : EXP × EXP → EXP'
*, /, +, -       : EXP × EXP → EXP'
nbre     : integer → EXP'
vrai, faux : → EXP'
chaine   : "string" → ID
omega    : → EXP'
```

avec **EXP = EXP' ∪ ID**.

> **`omega` (noté `w`)** est la valeur indéfinie. Une déclaration sans initialiseur (`int x ;`) produit `var(entier, ident(x), omega)`.

> **Note :** `tab` a pour type de retour `ID`, pas `EXP'`. C'est ce qui permet à `t[i]` d'apparaître à gauche d'une affectation comme à droite.

---

## 4. L'état mémoire

C'est le cœur du sujet, et la partie la moins évidente.

### Structure

Un état mémoire `m ∈ MEM` est une **pile de quadruplets** :

```
QUAD = ID × VAL × OBJ × SORTE
```

| Champ | Sens | Valeurs |
|---|---|---|
| **ID** | identificateur | nom, ou `w` pour les valeurs anonymes |
| **VAL** | valeur associée | entier, booléen, adresse, AST de méthode, `w` |
| **OBJ** | nature de l'objet | `var`, `cst`, `vcst`, `tab`, `meth` |
| **SORTE** | type | `entier`, `booléen`, `rien` |

Notation : `<i, v, o, t> . m` = quadruplet empilé au-dessus de `m`. `[]` = mémoire vide.

**Propriété clé :** c'est un compromis entre pile et table des symboles. La pile donne le **masquage** (portées imbriquées : la recherche s'arrête au premier `i` trouvé en partant du sommet), la table donne l'**accès direct aux attributs**.

**Le tas** est séparé et sert aux tableaux : `DeclTab` stocke dans le quadruplet une référence vers le tas (`CréerTas`).

### Les cinq natures d'objet

- `var` — variable ordinaire, réaffectable
- `cst` — constante initialisée, **non réaffectable**
- `vcst` — constante déclarée sans valeur ; la première affectation la transforme en `cst`
- `tab` — tableau ; la valeur est une référence dans le tas
- `meth` — méthode ; la valeur est l'AST de la méthode (interpréteur) ou son adresse de début (JajaCode)

### Le quadruplet anonyme

`<w, v, cst, w>` est le quadruplet de **valeur temporaire** : résultat d'un calcul, argument d'appel, adresse de retour. C'est la « pile d'évaluation » du JajaCode, fondue dans la même structure que l'environnement. **C'est le point d'architecture le plus important à comprendre.**

### Opérations de pile

```
Créer     : → MEM                       []
Empiler   : QUAD × MEM → MEM            Empiler(q, m) = q.m
Dépiler   : MEM → MEM                   Dépiler(q.m) = m
Échanger  : MEM → MEM                   Échanger(q1.q2.m) = q2.q1.m
```

### Opérations de déclaration

```
DeclVar(i, v, t, m)  = <i, v, var, t> . m

DeclCst(i, v, t, m)  = si v == w  alors <i, v, vcst, t> . m
                                  sinon <i, v, cst, t> . m

DeclTab(i, v, t, m)  = <i, CréerTas(v, t, m), tab, t> . m

DeclMeth(i, v, t, m) = <i, v, meth, t> . m

IdentVal(i, t, [], s) = []
IdentVal(i, t, <i1,v1,o1,t1>.m, s) =
    si s == 0 alors <i, v1, var, t> . m
              sinon <i1,v1,o1,t1> . IdentVal(i, t, m, s-1)
```

> **`IdentVal` est subtile** : elle descend de `s` crans dans la pile, puis **renomme** le quadruplet trouvé en lui donnant l'identifiant `i` et le type `t`. C'est le mécanisme de « transformation d'une valeur temporaire en variable nommée ». Avec `s = 0`, elle nomme le sommet — c'est ce qui se passe pour toute déclaration `int x = e ;` : on évalue `e` (qui empile `<w,v,cst,w>`), puis on renomme ce quadruplet en `x`.

### Retrait

```
RetirerDecl(i, []) = []
RetirerDecl(i, <i1,v1,o,t>.m) =
    si i == i1 alors (si o == tab alors RetirerTas(v1,t)) puis m
               sinon <i1,v1,o,t> . RetirerDecl(i, m)
```

### Affectation

```
AffecterVal(i, v, <i1,v1,o,t>.m) =
    si i ≠ i1        → <i1,v1,o,t> . AffecterVal(i, v, m)
    sinon si o==vcst → <i, v, cst, t> . m        (1re affectation d'une constante)
    sinon si o==cst  → m                          (⚠ affectation d'une constante)
    sinon si o==tab  → AjouterRef(v,t), RetirerTas(v1,t)
    sinon            → <i, v, o, t> . m

AffecterValT(i, v, ind, <i1,v1,o,t>.m) =
    si i ≠ i1 → <i1,v1,o,t> . AffecterValT(i, v, ind, m)
    sinon     → AffecterTas(v1, ind, v, m)

AffecterType(i, t, <i1,v1,o,t1>.m) =
    si i == i1 alors <i, v1, o, t> . m
               sinon <i1,v1,o,t1> . AffecterType(i, t, m)
```

> ⚠ Le cas `o == cst` renvoie `m` sans le quadruplet — la définition du cours est ambiguë ici. **En pratique : levez une erreur** « affectation à une constante ». Documentez ce choix.

### Passage de paramètres

```
ExpParam(lexp, ent, m) =
    si lexp ≠ exnil et ent ≠ enil
      alors ExpParam(le1, ents, DeclVar(i, v, t, m))
      sinon m
    où  m ⊢ e ⇒ v,  lexp = listexp(e, le1),  ent = entêtes(entête(t,i), ents)
```

Parcourt en parallèle la liste d'expressions et la liste d'en-têtes, évalue chaque argument et le déclare comme variable locale.

### Accès

```
Val(i, [])  = w        Val(i, <i1,v1,o,t>.m)  = si i==i1 alors v1 sinon Val(i,m)
ValT(i, ind, m)        → ValeurTas(v1, ind, m)
Long(i, m)             → LongTas(v1, m)
Objet(i, m)            → o
Sorte(i, m)            → t
Paramètre(i, m)        → ents   (si o == meth)
Déclaration(i, m)      → dvs    (si o == meth)
Corps(i, m)            → iss    (si o == meth)
```

`Paramètre`, `Déclaration` et `Corps` décomposent la valeur `méthode(t,i,ents,dvs,iss)` stockée dans le quadruplet.

---

## 5. Sémantique interprétative MiniJaja

Trois jugements :

| Jugement | Sens |
|---|---|
| `m ⊢ instr → m'` | exécution : transforme un état mémoire |
| `m ⊢ᵉᵛᵃˡ e ⇒ v` | évaluation : produit une valeur |
| `m ⊢ʳᵉᵗʳᵃᶦᵗ decls → m'` | retrait : dépile les déclarations en sortie de portée |

### Programme et déclarations

```
[classe]  DeclVar(i,w,w,[]) ⊢ dss → m1
          m1 ⊢ mma → m2
          m2 ⊢ʳᵉᵗʳᵃᶦᵗ dss → m3
          ────────────────────────────────────────────────
          [] ⊢ classe(ident(i), dss, mma) → RetirerDecl(ident(i), m3)

[decls]   m ⊢ ds → m1,  m1 ⊢ dss → m2   ⟹   m ⊢ decls(ds,dss) → m2
[vars]    idem
[vnil]    m ⊢ vnil → m

[var]     m ⊢ᵉᵛᵃˡ e ⇒ v   ⟹   m ⊢ var(t,ident(i),e) → DeclVar(i,v,t,m)
[cst]     m ⊢ᵉᵛᵃˡ e ⇒ v   ⟹   m ⊢ cst(t,ident(i),e) → DeclCst(i,v,t,m)
[tableau] m ⊢ᵉᵛᵃˡ e ⇒ v   ⟹   m ⊢ tableau(t,ident(i),e) → DeclTab(i,v,t,m)
[méthode] m ⊢ méthode(t,ident(i),ent,dvs,iss)
              → DeclMeth(i, méthode(t,ident(i),ent,dvs,iss), t, m)

[main]    m ⊢ dvs → m1,  m1 ⊢ iss → m2,  m2 ⊢ʳᵉᵗʳᵃᶦᵗ dvs → m3
          ⟹  m ⊢ main(dvs,iss) → m3
```

> **`DeclVar(i, w, w, [])` en tête de [classe]** crée un quadruplet portant le nom de la classe. Il sert de réceptacle à la valeur de retour (voir `VariableClasse` ci-dessous).

### Instructions

```
[instrs]        m ⊢ is → m1, m1 ⊢ iss → m2  ⟹  m ⊢ instrs(is,iss) → m2
[inil]          m ⊢ inil → m

[affectation]   m ⊢ᵉᵛᵃˡ e ⇒ v  ⟹  m ⊢ affectation(ident(i),e) → AffecterVal(i,v,m)
[affectationT]  m ⊢ᵉᵛᵃˡ e ⇒ v, m ⊢ᵉᵛᵃˡ e1 ⇒ ind
                ⟹  m ⊢ affectation(tab(ident(i),e1),e) → AffecterValT(i,ind,v,m)

[incrément]     m ⊢ incrément(ident(i)) → AffecterVal(i, Val(i,m)+1, m)
[incrémentT]    m ⊢ᵉᵛᵃˡ e ⇒ ind
                ⟹ m ⊢ incrément(tab(ident(i),e)) → AffecterValT(i,ind,ValT(i,ind,m)+1,m)

[somme]         m ⊢ᵉᵛᵃˡ e ⇒ v  ⟹  m ⊢ somme(ident(i),e) → AffecterVal(i, Val(i,m)+v, m)
[sommeT]        analogue avec AffecterValT

[retour]        m ⊢ᵉᵛᵃˡ e ⇒ v
                ⟹  m ⊢ retour(e) → AffecterVal(VariableClasse(m), v, m)

[ecrire]        m ⊢ᵉᵛᵃˡ e ⇒ v  ⟹  m ⊢ ecrire(e) → Afficher(v,m)

[sivrai]        m ⊢ᵉᵛᵃˡ e ⇒ true,  m ⊢ iss → m1   ⟹  m ⊢ si(e,iss,iss1) → m1
[sifaux]        m ⊢ᵉᵛᵃˡ e ⇒ false, m ⊢ iss1 → m1  ⟹  m ⊢ si(e,iss,iss1) → m1

[tantquevrai]   m ⊢ᵉᵛᵃˡ e ⇒ true, m ⊢ iss → m1, m1 ⊢ tantque(e,iss) → m2
                ⟹  m ⊢ tantque(e,iss) → m2
[tantquefaux]   m ⊢ᵉᵛᵃˡ e ⇒ false  ⟹  m ⊢ tantque(e,iss) → m

[appelI]        ExpParam(lexp, Paramètre(i,m), m) ⊢ Déclaration(i,m) → m1
                m1 ⊢ Corps(i,m) → m2
                m2 ⊢ʳᵉᵗʳᵃᶦᵗ Déclaration(i,m) → m3
                m3 ⊢ʳᵉᵗʳᵃᶦᵗ Paramètre(i,m) → m4
                ────────────────────────────────────────
                m ⊢ appelI(ident(i), lexp) → m4
```

> **`si(e, iss, iss1)` : le 2ᵉ argument est la branche `then`**, le 3ᵉ la branche `else`.

> **`VariableClasse(m)`** n'est pas définie dans le cours. C'est l'identifiant du quadruplet créé au début de `[classe]`, qui porte le nom de la classe et sert de case de retour. **À définir vous-même** ; documentez le choix.

### Évaluation des expressions

```
[ident]   m ⊢ᵉᵛᵃˡ ident(i) ⇒ Val(i,m)
[tab]     m ⊢ᵉᵛᵃˡ e ⇒ v  ⟹  m ⊢ᵉᵛᵃˡ tab(ident(i),e) ⇒ ValT(i,v,m)
[nbre]    m ⊢ᵉᵛᵃˡ nbre(n) ⇒ n
[vrai]    ⇒ true       [faux] ⇒ false       [omega] ⇒ w
[chaine]  m ⊢ᵉᵛᵃˡ chaine(s) ⇒ s
[non]     m ⊢ᵉᵛᵃˡ e ⇒ v  ⟹  non(e) ⇒ ¬v
[moins]   m ⊢ᵉᵛᵃˡ e ⇒ v  ⟹  moins(e) ⇒ −v
[op2]     m ⊢ᵉᵛᵃˡ e ⇒ v, m ⊢ᵉᵛᵃˡ e1 ⇒ v1  ⟹  op2(e,e1) ⇒ v op v1

[appelE]  m ⊢ appelI(ident(i), lexp) → m1
          m1 ⊢ᵉᵛᵃˡ VariableClasse(m1) ⇒ v
          ────────────────────────────────
          m ⊢ᵉᵛᵃˡ appelE(ident(i), lexp) ⇒ v
```

> **`appelE` est défini à partir de `appelI`** : on exécute l'appel comme une instruction, puis on lit la valeur déposée dans la variable de classe par `retour`.

### Retrait

```
[rdecls]    m ⊢ʳ dss → m1, m1 ⊢ʳ ds → m2  ⟹  m ⊢ʳ decls(ds,dss) → m2
[rvars]     idem                          (⚠ ordre inversé : la queue d'abord)
[rvnil]     m ⊢ʳ vnil → m
[rvar] [rcst] [rentête]  m ⊢ʳ var(t,ident(i),e) → RetirerDecl(i, m)
[rtableau]  m ⊢ʳ tableau(t,ident(i),e) → RetirerDecl(i, m)
[rméthode]  m ⊢ʳ méthode(t,ident(i),en,dvs,ins) → RetirerDecl(i, m)
```

> **L'ordre est inversé** par rapport à la déclaration : on retire dans l'ordre inverse d'empilement. C'est un détail facile à rater qui casse tout.

---

## 6. La machine virtuelle JajaCode

Une VM composée de :
- un **jeu d'instructions** (JajaCode)
- une **pile** (même structure `MEM` que ci-dessus)
- un **tas** pour les tableaux

La pile contient : associations identificateur/valeur, valeurs résultats, adresses d'instructions, valeurs effectives.

**État de la machine :** `<MEM, ADR>` où `ADR = ℕ` est le compteur ordinal.

### Jeu d'instructions

| Catégorie | Instructions |
|---|---|
| Contrôle machine | `init`, `nop`, `jcstop` |
| Déclaration | `new(ident,type,sorte,val)`, `newarray(ident,type)` |
| Pile | `push(valeur)`, `pop`, `swap` |
| Variables | `load(ident)`, `store(ident)`, `aload(ident)`, `astore(ident)` |
| Incrément | `inc(ident)`, `ainc(ident)` |
| Tableaux | `length(ident)` |
| Appels | `invoke(ident)`, `return` |
| Saut | `goto(adresse)`, `if(adresse)` |
| Sortie | `write`, `writeln` |
| Opérateurs | `add`, `sub`, `mul`, `div`, `cmp`, `sup`, `and`, `or`, `neg`, `not` |

Préfixe `a` = version tableau (`aload`, `astore`, `ainc`).
`cmp` = égalité, `sup` = supériorité.

### Syntaxe abstraite JajaCode

```
JajaCode : ADR × JCODE × JCODES → JCODES
jcnil    : → JCODES
init, pop, nop, jcstop, return, write, writeln, swap, oper : → JCODE
invoke, inc, ainc, length : ID → JCODE
load, store, aload, astore : ID → JCODE
new      : ID × TYPE × SORTE × JCVAL → JCODE
newarray : ID × TYPE → JCODE
push     : JCVAL → JCODE
goto, if : ADR → JCODE
jcident  : string → ID
jcnbre   : entier → JCVAL
jcvrai, jcfaux, jcw : → JCVAL
```

Un programme JajaCode est une liste `adresse : instruction`.

### Sémantique des instructions

Notation `<m, a> ⊢ instr ↠ <m', a'>`.

```
[init]      <m,a> ⊢ init ↠ <[], a+1>              (réinitialise la mémoire)
[jcstop]    <m,a> ⊢ jcstop ↠ <m, ⊥>               (arrêt)
[nop]       <m,a> ⊢ nop ↠ <m, a+1>
[swap]      <q1.q2.m,a> ⊢ swap ↠ <q2.q1.m, a+1>
[pop]       <q.m,a> ⊢ pop ↠ <m, a+1>
[push]      <m,a> ⊢ push(v) ↠ <<w,v,cst,w>.m, a+1>

[newV]      <m,a> ⊢ new(i,t,var,s) ↠ <IdentVal(i,t,m,s), a+1>
[newC]      <<w,v,cst,w>.m,a> ⊢ new(i,t,cst,0) ↠ <DeclCst(i,v,t,m), a+1>
[newM]      <<w,v,cst,w>.m,a> ⊢ new(i,t,meth,0) ↠ <DeclMeth(i,v,t,m), a+1>
[newarray]  <<w,v,cst,w>.m,a> ⊢ newarray(i,t) ↠ <DeclTab(i,v,t,m), a+1>

[load]      <m,a> ⊢ load(i) ↠ <<w,Val(i,m),cst,w>.m, a+1>
[aload]     <<w,ind,cst,w>.m,a> ⊢ aload(i) ↠ <<w,ValT(i,ind,m),cst,w>.m, a+1>
[store]     <<w,v,cst,w>.m,a> ⊢ store(i) ↠ <AffecterVal(i,v,m), a+1>
[astore]    <<w,v,cst,w>.<w,ind,cst,w>.m,a> ⊢ astore(i)
                ↠ <AffecterValT(i,ind,v,m), a+1>

[inc]       <<w,v,cst,w>.m,a> ⊢ inc(i) ↠ <AffecterVal(i,Val(i,m)+v,m), a+1>
[ainc]      <<w,v,cst,w>.<w,ind,cst,w>.m,a> ⊢ ainc(i)
                ↠ <AffecterValT(i,ind,ValT(i,ind,m)+v,m), a+1>

[goto]      <m,a> ⊢ goto(a1) ↠ <m, a1>
[iftrue]    <<w,true,cst,w>.m,a> ⊢ if(a1) ↠ <m, a1>
[iffalse]   <<w,false,cst,w>.m,a> ⊢ if(a1) ↠ <m, a+1>

[invoke]    <m,a> ⊢ invoke(i) ↠ <<w,a+1,cst,w>.m, Val(i,m)>
[return]    <<w,a1,cst,w>.m,a> ⊢ return ↠ <m, a1>

[write]     <<w,v,cst,w>.m,a> ⊢ write ↠ <Afficher(v,m), a+1>
[writeln]   <<w,v,cst,w>.m,a> ⊢ writeln ↠ <AfficherLn(v,m), a+1>

[op2]       <<w,v2,cst,w>.<w,v1,cst,w>.m,a> ⊢ oper2
                ↠ <<w, v1 oper2 v2, cst,w>.m, a+1>
[op1]       <<w,v1,cst,w>.m,a> ⊢ oper1 ↠ <<w, oper1 v1, cst,w>.m, a+1>
```

> **Points clés à ne pas rater :**
> - `invoke(i)` empile l'**adresse de retour** `a+1` puis saute à `Val(i,m)`, qui est l'adresse du corps de la méthode.
> - `return` dépile l'adresse de retour et y saute. D'où le `swap` juste avant : il fait passer la valeur de retour sous l'adresse.
> - `if(a1)` saute **si vrai** (l'inverse d'un `ifeq` JVM). D'où le `not` inséré avant le `if` dans la boucle `tantque`.
> - Pour `op2`, l'opérande **droite est au sommet** : `v1 op v2` avec `v2` dépilé en premier. Attention à `sub` et `div`.

---

## 7. Compilation MiniJaja → JajaCode

### Opérateurs de concaténation

```
⊕  : JCODES × JCODES → JCODES     (concaténation de deux séquences)
⊕G : JCODE × JCODES → JCODES      (ajout d'une instruction à gauche)
⊕D : JCODES × JCODE → JCODES      (ajout d'une instruction à droite)
```

Élément neutre : `jcnil`.

### Forme des règles

```
n ⊢ construction ⇒ {code, taille}
```

`n` est l'adresse de départ, `taille` le nombre d'instructions générées. **Le calcul des adresses est la principale source de bugs** : chaque règle doit être vérifiée arithmétiquement.

### Programme

```
[cclasse]   n+1 ⊢ dss ⇒ {pdss, ndss}
            n+ndss+1 ⊢ mma ⇒ {pmma, nmma}
            n+ndss+nmma+1 ⊢ʳ dss ⇒ {prdss, nrdss}
            ⟹ {init ⊕G (pdss ⊕ pmma ⊕ prdss) ⊕D pop ⊕D jcstop,
                ndss+nmma+nrdss+3}

[cmain]     n ⊢ dvs ⇒ {pdvs, ndvs}
            n+ndvs ⊢ iss ⇒ {piss, niss}
            n+ndvs+niss+1 ⊢ʳ dvs ⇒ {prdvs, nrdvs}
            ⟹ {pdvs ⊕ piss ⊕ (push(0) ⊕G prdvs), ndvs+niss+nrdvs+1}
```

### Déclarations

```
[cdecls]    n ⊢ ds ⇒ {pds,nds}, n+nds ⊢ dss ⇒ {pdss,ndss}
            ⟹ {pds ⊕ pdss, nds+ndss}
[cvars]     idem
[cvnil]     {jcnil, 0}

[cvar]      n ⊢ e ⇒ {pe,ne}  ⟹  {pe ⊕D new(i,t,var,0), ne+1}
[ccst]      n ⊢ e ⇒ {pe,ne}  ⟹  {pe ⊕D new(i,t,cst,0), ne+1}
[ctableau]  n ⊢ e ⇒ {pe,ne}  ⟹  {pe ⊕D newarray(i,t), ne+1}

[centêtes]  n ⊢ ens ⇒ {pens,nens}, n+nens ⊢ en ⇒ {pen,nen}
            ⟹ {pens ⊕ pen, nens+nen}      (⚠ queue compilée AVANT la tête)
[centête]   {new(i,t,var,k) ⊕G jcnil, 1}
```

> **Le `k` de `[centête]`** est la profondeur passée à `IdentVal` : le nombre de quadruplets à sauter dans la pile pour atteindre l'argument correspondant. Au moment de l'entrée dans la méthode, la pile contient l'adresse de retour au sommet, puis les arguments. Pour un paramètre unique, `k = 1` (on saute l'adresse de retour). **Le cours ne donne pas la formule générale — à établir vous-même** en fonction de la position du paramètre.

### Méthodes

```
[cméthode]  n+3 ⊢ ens ⇒ {pens, nens}
            n+nens+3 ⊢ dvs ⇒ {pdvs, ndvs}
            n+nens+ndvs+3 ⊢ iss ⇒ {piss, niss}
            n+nens+ndvs+niss+3 ⊢ʳ dvs ⇒ {prdvs, nrdvs}
            ⟹ { push(n+3)
                ⊕D new(i,t,meth,0)
                ⊕D goto(n+nens+ndvs+niss+nrdvs+5)
                ⊕ pens ⊕ pdvs ⊕ piss ⊕ prdvs
                ⊕D swap ⊕D return,
                nens+ndvs+niss+nrdvs+5 }
```

**Décomposition des adresses :**

| Adresse | Instruction | Rôle |
|---|---|---|
| `n` | `push(n+3)` | empile l'adresse du corps |
| `n+1` | `new(i,t,meth,0)` | déclare la méthode, valeur = adresse du corps |
| `n+2` | `goto(fin)` | saute par-dessus le corps à la déclaration |
| `n+3` … | `pens` puis `pdvs` puis `piss` puis `prdvs` | corps de la méthode |
| avant-dernière | `swap` | fait passer la valeur de retour sous l'adresse de retour |
| dernière | `return` | dépile l'adresse et y saute |

La variante `[cméthodeR]` insère `push(0) ⊕G prdvs` et compte une instruction de plus — c'est le cas d'une méthode `void`, où il faut fournir une valeur de retour factice.

### Instructions

```
[cinstrs]   n ⊢ is ⇒ {pis,nis}, n+nis ⊢ iss ⇒ {piss,niss} ⟹ {pis ⊕ piss, nis+niss}

[caffecte]  n ⊢ e ⇒ {pe,ne}  ⟹  {pe ⊕D store(i), ne+1}
[caffecteT] pe ⊕ pe1 ⊕D astore(i),  ne+ne1+1
[csomme]    {pe ⊕D inc(i), ne+1}
[csommeT]   {pe ⊕ pe1 ⊕D ainc(i), ne+ne1+1}
[cinc]      {push(1) ⊕D inc(i), 2}
[cincT]     {pe ⊕D push(1) ⊕D ainc(i), ne+2}
[cretour]   {pe, ne}                       (⚠ aucune instruction ajoutée)
[cecrire]   {pe ⊕D write, ne+1}

[cappelE]   n ⊢ lexp ⇒ {plexp,nlexp}
            n+nlexp+1 ⊢ʳ lexp ⇒ {prlexp,nrlexp}
            ⟹ {plexp ⊕D invoke(i) ⊕ prlexp, nlexp+nrlexp+1}

[cappelI]   idem ⊕D pop,  nlexp+nrlexp+2
            (le pop supprime la valeur de retour inutilisée)
```

#### Conditionnelle

```
[csi]   n ⊢ e ⇒ {pe,ne}
        n+ne+1 ⊢ s1 ⇒ {ps1,ns1}
        n+ne+ns1+2 ⊢ s ⇒ {ps,ns}
        ⟹ {(pe ⊕D if(n+ne+ns1+2)) ⊕ ps1 ⊕D goto(n+ne+ns1+ns+2) ⊕ ps,
            ne+ns1+ns+2}
```

Disposition mémoire :

```
n           .. n+ne-1        : condition
n+ne                          : if(→ then)
n+ne+1      .. n+ne+ns1       : branche ELSE
n+ne+ns1+1                    : goto(fin)
n+ne+ns1+2  .. n+ne+ns1+ns+1  : branche THEN
n+ne+ns1+ns+2                 : fin
```

> **La branche `else` est émise avant la branche `then`.** C'est l'inverse de la disposition habituelle, conséquence du fait que `if` saute quand la condition est vraie.

#### Boucle

```
[ctantque]  n ⊢ e ⇒ {pe,ne}
            n+ne+2 ⊢ iss ⇒ {piss,niss}
            ⟹ {(pe ⊕D not) ⊕D if(n+ne+niss+3) ⊕ piss ⊕D goto(n),
                ne+niss+3}
```

Disposition mémoire :

```
n           .. n+ne-1         : condition          ◄──┐
n+ne                           : not                  │
n+ne+1                         : if(→ sortie)         │
n+ne+2      .. n+ne+niss+1     : corps                │
n+ne+niss+2                    : goto(n) ──────────────┘
n+ne+niss+3                    : sortie
```

### Expressions

```
[cident]    {load(i), 1}
[ctab]      {pe ⊕ aload(i), ne+1}
[cnbre]     {push(n), 1}
[cchaine]   {push(s), 1}
[cvrai]     {push(jcvrai), 1}
[cfaux]     {push(jcfaux), 1}
[cop1]      {pe ⊕D op1, ne+1}
[cop2]      n ⊢ e1 ⇒ {pe1,ne1}, n+ne1 ⊢ e2 ⇒ {pe2,ne2}
            ⟹ {(pe1 ⊕ pe2) ⊕D op2, ne1+ne2+1}
[clexp]     {pexp ⊕ plexp, nexp+nlexp}
```

### Retrait des déclarations

```
[crvnil] [crenil] [crexnil]  {jcnil, 0}
[crvar] [crcst] [crméthode]  {swap ⊕D pop, 2}
[crdecls]   n ⊢ʳ dss ⇒ {prdss,nrdss}, n+nrdss ⊢ʳ ds ⇒ {prds,nrds}
            ⟹ {prdss ⊕ prds, nrdss+nrds}
[crvars]    idem
[crlexp]    {swap ⊕D pop ⊕ prlexp, nrlexp+2}
```

> **Le motif `swap ; pop`** revient partout : la valeur utile est au sommet, la déclaration à retirer juste en dessous. On les échange, puis on dépile.

---

## 8. Exemple complet annoté

```java
class C {
  final int x = 0 ;

  int f(int p) {
    return p
  } ;

  main {
    x = 3 ;
    x += f(x) ;
  }
}
```

| Adr | JajaCode | Commentaire |
|---|---|---|
| 1 | `init` | mémoire vide |
| 2 | `push(0)` | valeur de `x` |
| 3 | `new(x,entier,var,0)` | nomme le sommet en `x` |
| 4 | `push(7)` | adresse du corps de `f` |
| 5 | `new(f,entier,meth,0)` | déclare `f`, valeur = 7 |
| 6 | `goto(11)` | saute le corps |
| 7 | `new(p,entier,var,1)` | nomme l'argument (saut de l'adresse de retour) |
| 8 | `load(p)` | valeur de retour au sommet |
| 9 | `swap` | adresse de retour ↔ valeur |
| 10 | `return` | saut à l'adresse de retour |
| 11 | `push(3)` | `x = 3` |
| 12 | `store(x)` | |
| 13 | `load(x)` | argument de `f(x)` |
| 14 | `invoke(f)` | empile l'adresse 15, saute en 7 |
| 15 | `swap` | retrait du paramètre |
| 16 | `pop` | |
| 17 | `inc(x)` | `x += résultat` |
| 18 | `push(0)` | valeur factice pour méthode void |
| 19 | `swap` | retrait de `f` |
| 20 | `pop` | |
| 21 | `swap` | retrait de `x` |
| 22 | `pop` | |
| 23 | `pop` | retrait du 0 du main |
| 24 | `jcstop` | |

> **Cet exemple est votre meilleur test de non-régression.** Codez-le comme test unitaire dès que le compilateur produit ses premières instructions.

---

## 9. Contrôle de type

**Système de typage statique.** Trois familles de contrôles :

- **Contrôle de type** — incompatibilité des opérandes
- **Contrôle du flot d'exécution** — transferts de contrôle valides
- **Contrôle d'unicité/répétition** — un nom apparaît un nombre fixé de fois

### Contrôles à implémenter pour MiniJaja

| Contrôle | Nature |
|---|---|
| Pré-déclaration des variables et méthodes | statique |
| Nombre et type des paramètres d'appel | statique |
| Opérateurs : `&&`,`\|\|`,`!` sur booléens ; `+`,`-`,`*`,`/` sur entiers | statique |
| Type de la valeur de `return` cohérent avec la signature | statique |
| Condition de `if`/`while` booléenne | statique |
| **Indice de tableau dans les bornes** | **dynamique** |

Pour JajaCode : contrôle statique des adresses, variables, méthodes, opérateurs ; contrôle dynamique de la pile.

### Mise en œuvre

Deux jugements, parallèles à ceux de la sémantique interprétative :

```
MEM ⊢ INSTR → MEM        (parcours de l'AST, mémoire de types)
MEM ⊢ᵉᵛᵃˡ EXP ⇒ SORTE     (propagation du type)
```

Autrement dit : **le contrôleur de type est le même parcours que l'interpréteur, mais en propageant des types au lieu de valeurs.** Vous pouvez factoriser via un visiteur générique. `AffecterType` sert précisément à ça.

Réalisable en une passe (pas de surcharge ni de polymorphisme dans MiniJaja).

---

## 10. Correction du compilateur

Un compilateur est **correct** si les sémantiques interprétatives du programme source et du programme compilé sont équivalentes :

1. identité des calculs (suite d'états mémoire),
2. identité des états mémoire finaux,
3. identité des valeurs de résultat.

**Équivalence** — `équiv(m, m′, i)` ⟺ `Val(i,m) = Val(i,m′)`.

Il faut prouver : ∀ programme `PN`, ∀ `m ∈ MEM`, ∀ `i ∈ ID` :

```
m ⊢ PN → m′        (sémantique MiniJaja)
a ⊢ PN ⇒ {PC, n}   (compilation)
<m,a> ⊢ PC ↠ <m″,a′>   (exécution JajaCode)
────────────────────────────────
équiv(m′, m″, i)
```

**En pratique, pour un projet :** ne cherchez pas la preuve formelle. Implémentez un **test différentiel** — pour chaque programme d'exemple, exécutez l'interpréteur MiniJaja et l'interpréteur JajaCode, et vérifiez que les états mémoire finaux coïncident. C'est la version exécutable de la même idée, et c'est excellent en démonstration.

---

## 11. Optimisation (extension facultative)

Trois critères : préserver la sémantique, accélérer, rapport coût/gain favorable.

### Graphe de flot de contrôle

Partition d'une séquence JajaCode en blocs de base. Instructions de tête de bloc :
1. la première instruction,
2. toute instruction atteinte par un branchement,
3. toute instruction suivant un branchement.

Un bloc va d'une tête jusqu'à la tête suivante (exclue).

### Forme SSA

Chaque variable modifiée reçoit un numéro, ce qui permet d'identifier définitions et utilisations, puis de réorganiser les calculs.

### Allocation de registres

Étiquetage de l'arbre (algorithme de Sethi-Ullman) : une feuille vaut 1 si elle est fils gauche, 0 sinon ; un nœud interne vaut `max(Étiquette(nᵢ) + i − 1)` sur ses fils ordonnés par étiquette décroissante.

> **Pour votre projet :** l'optimisation est le premier chapitre à couper si le temps manque. Une seule optimisation simple bien faite — élimination du code mort ou pliage de constantes — vaut mieux qu'une tentative d'allocation de registres inachevée.

---

## 12. Notes d'implémentation

### Architecture Java suggérée

```
fr.<vous>.minijaja
├── lexer/          Token, TokenType, Lexer
├── ast/            hiérarchie de nœuds + interface Visitor
├── parser/         Parser (descente récursive)
├── memory/         Quad, Memory, Heap
├── typecheck/      TypeChecker implements Visitor<Sorte>
├── interp/         MjjInterpreter implements Visitor<Void>
├── compiler/       JajaCodeCompiler implements Visitor<CompiledFragment>
├── jajacode/       JCode (sealed interface), JajaCodeVM
└── error/          MjjError avec ligne/colonne
```

**`CompiledFragment`** = `record CompiledFragment(List<JCode> code, int size)`. C'est le `{code, taille}` des règles de compilation. Les opérateurs `⊕`, `⊕G`, `⊕D` deviennent des méthodes de ce record.

Utilisez le **pattern Visiteur** partout : le même parcours d'AST sert au typage, à l'interprétation et à la compilation. Les records Java et le pattern matching sur types scellés rendent ça très propre.

### Ordre de développement recommandé

1. Lexer + tests
2. Parser + AST + `--dump-ast`
3. **Interpréteur MiniJaja** — permet d'exécuter des programmes très tôt et de valider le front-end
4. Contrôleur de type
5. Compilateur vers JajaCode + `--dump-jajacode`
6. VM JajaCode
7. Test différentiel interpréteur vs VM
8. Débogueur en ligne de commande (pas-à-pas, dump pile/tas)

### Points de vigilance

- **Le calcul des adresses** est la source de bugs numéro un. Écrivez un test par règle de compilation qui vérifie `taille == code.size()`.
- **L'ordre des opérandes** dans `op2` : la droite est au sommet de pile.
- **L'ordre inversé** du retrait des déclarations et de la compilation des en-têtes.
- **La disposition else-avant-then** dans `[csi]`.
- **`VariableClasse`** et le **`k` de `[centête]`** ne sont pas spécifiés : définissez-les, documentez-les.
- Le cas `AffecterVal` sur une constante est ambigu : levez une erreur.

### Ce qui n'est pas spécifié et vous appartient

- conventions lexicales (identifiants, nombres, commentaires)
- syntaxe concrète de `write`, `writeln`, `length`, littéraux chaîne
- représentation exacte du tas et politique du ramasse-miettes
- format textuel du JajaCode (pour `--dump`)
- messages d'erreur et stratégie de récupération

C'est une bonne nouvelle : ce sont autant de choix d'ingénierie à documenter dans votre README, et ce sont eux qui rendront votre projet non comparable aux dépôts existants.
