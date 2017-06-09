#!/bin/sh

git submodule update --init

cd pf

ant || exit -1

cd ..

ant || exit -1

cp out/artifacts/MotorDaemonManager_jar/MotorDaemonManager.jar hermes-manager.jar

echo "BUILD SUCCESSFULL !"