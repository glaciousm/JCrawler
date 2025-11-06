#!/bin/bash
# JCrawler Launcher for Linux/Mac
# This script runs the application using Maven's JavaFX plugin

echo "Starting JCrawler..."
echo

mvn javafx:run

if [ $? -ne 0 ]; then
    echo
    echo "ERROR: Failed to start JCrawler"
    echo "Make sure Maven is installed and in your PATH"
    echo
fi
