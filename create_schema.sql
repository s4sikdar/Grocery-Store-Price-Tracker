BEGIN;

-- use MySQL when running these commands

CREATE DATABASE IF NOT EXISTS grocery_prices;

USE grocery_prices;

CREATE TABLE IF NOT EXISTS fruits_and_vegetables (
	brand varchar(100),
	date_collected date,
	price varchar(100),
	product_title varchar(200),
	product_size varchar(100),
	store_chain_name varchar(100),
	township_location varchar(200),
	unit_price varchar(150)
);

CREATE TABLE IF NOT EXISTS dairy_and_eggs LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS pantry LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS meat LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS snacks_and_chips_and_candy LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS frozen_food LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS bakery LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS drinks LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS deli LIKE fruits_and_vegetables;

CREATE TABLE IF NOT EXISTS fish_and_seafood LIKE fruits_and_vegetables;

COMMIT;
