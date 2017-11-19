#!/bin/bash

set -e

dir=`dirname "${0}"`
cd "${dir}"
./gradlew classes
zip -d build/libs/DiscordSoundboard-1.4.11.jar net/dirtydeeds/\*
cd build/classes/main
zip -g ../../../build/libs/DiscordSoundboard-1.4.11.jar net/dirtydeeds/**/*
