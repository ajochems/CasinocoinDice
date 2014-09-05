- Create DiceDB database
	mysql -u root -p
- create database
	CREATE DATABASE dicedb;
	CREATE USER 'diceuser'@'%' IDENTIFIED BY 'diceuser1';
	GRANT ALL ON dicedb.* to 'diceuser'@'%';