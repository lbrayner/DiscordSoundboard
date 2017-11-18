#!/bin/sh

dir=`dirname "${0}"`
cd "${dir}"
mvn compile
zip -d build/libs/DiscordSoundboard-1.4.11.jar net/dirtydeeds/\*
cd target/classes
zip -g ../../build/libs/DiscordSoundboard-1.4.11.jar net/dirtydeeds/**/*
