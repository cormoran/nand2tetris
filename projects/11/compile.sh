#! /usr/bin/env bash
cd $(dirname $0)/../../
javac ./src/compiler/Compiler.java || exit -1
cd $(dirname $0)

for f in $(find . -name *.jack); do
    echo $f
    cp ../../tools/OS/* $(dirname $f) # copy os code
    java -ea -classpath ../.. src.compiler.Compiler $f > /dev/null || exit -1
done
