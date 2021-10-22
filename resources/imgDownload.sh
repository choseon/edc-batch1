#!/bin/bash
if ! [ -d ./kotra/binary/images/ ]; then
	mkdir ./kotra/binary/images/
else
	rm -r ./kotra/*
fi 
