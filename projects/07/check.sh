#! /usr/bin/env bash
cd $(dirname $0)/../../
javac ./src/vmtranslator/VMtranslator.java || exit -1
cd $(dirname $0)
for f in $(find . -name *.vm); do
    echo $f
    java -classpath ../.. src.vmtranslator.VMTranslator $f > /dev/null || exit -1
    ../../tools//CPUEmulator.sh ${f/vm/tst} || exit -1
done