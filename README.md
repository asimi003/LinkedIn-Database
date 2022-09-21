# LinkedIn-Database

Database Management Systems Project

## Description

A database using SQL, triggers, indexes, and Java to run a semi-decent console GUI.

## Requirements

Since this project was created on a local Windows computer. Some requirements must be met given you are also working on a Windows PC: 

  Postgres Server for Windows executable - Download and remember the port number and password if any. If using a different port number than the default
  (5432), update the Java code.
  Postgres JDBC Driver JAR API library - Make sure your IDE reads this as a library.

Make sure the libraries in java/lib are recognized by your IDE.

Change the absolute path data CSV files in load_data.sql

## Note

All SQL scripts are located in the sql/src folder. This folder has the create_index, create_tables, create_trigger, and load_data files used to load our data and be able to use it effectively.

All Java source code is located in the java/src folder. The file is convoluted but is documented thoroughly.

## Relational Database Design Diagram

![Database Diagram 1](https://user-images.githubusercontent.com/97551999/191624107-b879dbec-30d1-428c-8de2-4d012f9a5574.png)
