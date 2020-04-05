#! /usr/bin/env bash
cd $(dirname $0)/../../
javac src/assembler/Assembler.java || exit -1
cd $(dirname $0)
for f in $(find . -name *.asm); do
    java -classpath ../.. src.assembler.Assembler $f > /dev/null || exit -1
    if diff "${f/asm/hack}" "${f/asm/hack.correct}" > /dev/null; then
        printf "[OK] $f\n"
    else
        printf "[Fail] $f\n"
        diff "${f/asm/hack}" "${f/asm/hack.correct}"
    fi
done