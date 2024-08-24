BEGIN;

-- recommended to use MySQL when running these commands
-- these commands will clear the tables of all current entries

USE grocery_prices;

TRUNCATE TABLE fruits_and_vegetables;

TRUNCATE TABLE dairy_and_eggs;

TRUNCATE TABLE pantry;

TRUNCATE TABLE meat;

TRUNCATE TABLE snacks_and_chips_and_candy;

TRUNCATE TABLE frozen_food;

TRUNCATE TABLE bakery;

TRUNCATE TABLE drinks;

TRUNCATE TABLE deli;

TRUNCATE TABLE fish_and_seafood;

COMMIT;
