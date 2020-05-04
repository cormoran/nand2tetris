#! /usr/bin/env bash
cd $(dirname $0)/../../
javac ./src/vmtranslator/VMtranslator.java || exit -1
cd $(dirname $0)
for f in $(find ./ProgramFlow -name *.vm); do
    echo $f
    java -classpath ../.. src.vmtranslator.VMTranslator $f ${f/vm/asm/} 1  > /dev/null || exit -1
    ../../tools/CPUEmulator.sh ${f/vm/tst} || exit -1
done

for f in $(find ./FunctionCalls/SimpleFunction -name *.vm); do
    echo $f
    java -classpath ../.. src.vmtranslator.VMTranslator $f ${f/vm/asm/} 1 > /dev/null || exit -1
    ../../tools/CPUEmulator.sh ${f/vm/tst} || exit -1
done


for t in FibonacciElement StaticsTest NestedCall; do
    f="./FunctionCalls/${t}"
    echo $f
    java -classpath ../.. src.vmtranslator.VMTranslator $f ${f}/`basename $f`.asm > /dev/null || exit -1    
    ../../tools/CPUEmulator.sh ${f}/`basename $f`.tst || exit -1
done