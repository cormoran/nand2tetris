#! /usr/bin/env bash
cd $(dirname $0)/../../
javac ./src/compiler/Tokenizer.java || exit -1
javac ./src/compiler/CompliationEngine.java || exit -1
cd $(dirname $0)

# check Tokenizer
for f in $(find . -name *.jack); do
    echo $f
    java -ea -classpath ../.. src.compiler.Tokenizer $f > /dev/null || exit -1
    ../../tools/TextComparer.sh ${f/.jack/T.xml} ${f/.jack/T.gen.xml} || exit -1
done
echo "[OK] Tokenizer"

# check CompliationEngine
for f in $(find . -name *.jack); do
    echo $f
    java -ea -classpath ../.. src.compiler.CompliationEngine $f > /dev/null || exit -1
    ../../tools/TextComparer.sh ${f/.jack/.xml} ${f/.jack/.gen.xml} || exit -1
done
echo "[OK] Tokenizer"