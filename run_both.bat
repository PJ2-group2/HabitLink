@echo off
start cmd /k "mvn exec:java -Dexec.mainClass=com.habit.server.HabitServer"
start cmd /k "mvn exec:java -Dexec.mainClass=com.habit.client.gui.HabitClientGUI"
