## Disclaimer
Bohužel neznám Azure takže nedokážu posoudit věci, co se ho týkají.

## Obecné poznámky
Moje poznámky v kódu jsou označeny **TODO**.

Jde o nový projekt, pokud k tomu není vážný důvod, nepoužil bych zastaralou verzi javy **1.8** ale 
minimálně verzi **11** nebo lépe **16**. Kód bývá kratší a přehlednější.

Projekt je relativně malý, je nějaký důvod rozdělit ho na 4 části? Navic se duplikují třídy. Dal bych 
vše do jednoho projektu.

## Maven
Pokud je potřeba to rozdělit, bude nutné přidat ještě jeden projekt s kódem společným pro ostaní projekty.
Projekt groupId by měl být stejný pro všechny a lišil by se jen artifactId. Tedy např.

* groupId: com.function.musicme.songfunctions
* artifactId: shared
* artifactId: project1
* artifactId: project2
* artifactId: project3
* artifactId: project4

Dále je vhodné mít na společném místě definovány dependencies, properties a ostatní 
položky **pom.xml**, které se opakují. Tedy mít společný "parent" pro pom.xml všech projektů. 
Samozřejmě mnohem snazší by bylo dát vše do jednoho projektu.

## Formátování
Formátování zdrojáku je dost individuální :-), ale vždy je dobré být konzistentní v celém projektu.
