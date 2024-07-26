#!/bin/bash

if ! ./build.sh; then
    echo "Bob der Baumeister failed to build"
    exit 1
fi

SECONDS=0
for i in {1..3}
do
    echo "Iteration $i"

	java -XX:+UseG1GC -XX:+UseStringDeduplication -cp "bin:bin/de/codecoverage/utils/*" de.codecoverage.utils.TestDriver > output.log 2>&1

    # Überprüfen auf "Exception" => Failed !
    if grep -q "Exception" output.log; then
        echo "Fehler im Iteration $i."
        exit 1
    fi
done
duration=$SECONDS
echo "Okay ... took $((duration / 60)) minutes and $((duration % 60)) seconds."

echo -e "\033[32;1;2m33GB\033[0m"
SECONDS=0
for i in {1..3}
do
    echo "Iteration $i"
    java -Xms33g -Xmx33g -XX:+UseG1GC -XX:+UseStringDeduplication -cp "bin:bin/de/codecoverage/utils/*" de.codecoverage.utils.TestDriver > output.log 2>&1

    # Überprüfen auf "Exception" => Failed !
    if grep -q "Exception" output.log; then
        echo "Fehler im Iteration $i."
        exit 1
    fi
done
duration=$SECONDS
echo "Okay ... took $((duration / 60)) minutes and $((duration % 60)) seconds."


echo "Okay"
