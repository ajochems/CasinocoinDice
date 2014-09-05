--
-- Table that contains the server settings
--
CREATE TABLE dicedb.settings
(
  id int(255) NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  value varchar(255),
  CONSTRAINT STS_PK PRIMARY KEY (id),
  CONSTRAINT STS_NAME_UC UNIQUE (name)
)
COMMENT = 'Server settings table';

INSERT INTO dicedb.settings (name, value) VALUES ('init_complete', 0);
INSERT INTO dicedb.settings (name, value) VALUES ('server_wallet_address', '');
INSERT INTO dicedb.settings (name, value) VALUES ('jackpot_wallet_address', '');
INSERT INTO dicedb.settings (name, value) VALUES ('jackpot_value', '0');
INSERT INTO dicedb.settings (name, value) VALUES ('confirmations', '25');
INSERT INTO dicedb.settings (name, value) VALUES ('house_edge_percentage', '2.50');
INSERT INTO dicedb.settings (name, value) VALUES ('maximum_bet_loss', '10000');

--
-- Table that contains the available bets to users
--
CREATE TABLE dicedb.available_bets
(
  id int(255) NOT NULL AUTO_INCREMENT,
  game_value int(10) NOT NULL,
  game_operator varchar(10),
  game_desc varchar(255) NOT NULL,
  coin_address varchar(255) NOT NULL,
  winning_odds decimal(5,3) NOT NULL,
  win_multiplier decimal(10,3) NOT NULL,
  min_bet decimal(20,8) NOT NULL,
  max_bet decimal(20,8) NOT NULL,
  jackpot_bet tinyint(1) NOT NULL DEFAULT '0',
  active tinyint(1) NOT NULL DEFAULT '1',
  CONSTRAINT ABS_PK PRIMARY KEY (id)
)
COMMENT = 'Table that contains all available bets';

--
-- The dice value is taken from the first 4 hex digits of the txid so that can be max FFFF = 65535
-- The game is the number that will win and that is based on the winning odds * 65535 
-- So winning odds of 95% is game 'lessthan 62259' as 95% * 65535 = 62258.25 which is lessthan 62259
-- The win multiplier decides the price which is (100% - house edge) / winning odds
-- So winning odds of 95% is (100 - 2.50) / 95 = 1.026
-- With these bets an average maximum loss per bet of 10000 coins is used
-- The min and max bets can be set depending on the amount of coins you can backup a bet with
-- If you change the maximum_bet_loss setting from the admin console the max_bet will be recalculated
-- BE AWARE -->> Changing the house_edge_percentage setting will need a recalculation of the winning multiplier
-- Only change this setting from the server admin console which will recalculate the multipliers
--
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (62260, '<=', 'Less or equal to 62260', '', 95.000, 1.025, 1, 10000);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (58982, '<', 'Less than 58982', '', 90.000, 1.083, 1, 9250);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (55705, '<', 'Less than 55705', '', 85.000, 1.147, 1, 8750);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (52429, '<', 'Less than 52429', '', 80.000, 1.219, 1, 8200);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (49152, '<', 'Less than 49152', '', 75.000, 1.300, 1, 7500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (42598, '<', 'Less than 42598', '', 65.000, 1.500, 1, 6500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (36045, '<', 'Less than 36045', '', 55.000, 1.773, 1, 5500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (29491, '<', 'Less than 29491', '', 45.000, 2.167, 1, 4500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (22938, '<', 'Less than 22938', '', 35.000, 2.786, 1, 3500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (16384, '<', 'Less than 16384', '', 25.000, 3.900, 1, 2500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (9831, '<','Less than 9831', '', 15.000, 6.500, 1, 1500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (3277, '<','Less than 3277', '', 5.000, 19.500, 1, 500);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (1639, '<','Less than 1639', '', 2.500, 39.000, 1, 250);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (1311, '<','Less than 1311', '', 2.000, 48.750, 1, 200);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (984, '<','Less than 984', '', 1.500, 65.000, 1, 150);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (656, '<','Less than 656', '', 1.000, 97.500, 1, 100);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (328, '<','Less than 328', '', 0.500, 195.000, 1, 50);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (164, '<','Less than 164', '', 0.250, 390.000, 1, 25);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (66, '<','Less than 66', '', 0.100, 975.000, 1, 10);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (33, '<','Less than 33', '', 0.050, 1950.000, 1, 5);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (17, '<','Less than 17', '', 0.025, 3900.000, 1, 2);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet) 
VALUES (7, '<','Less than 7', '', 0.010, 9750.000, 1, 1);
--
-- Jackpot Bets
--
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (62260, '<', 'Less than 62260 + Jackpot', '', 95.000, 1.025, 2, 10000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (58982, '<', 'Less than 58982 + Jackpot', '', 90.000, 1.083, 2, 9250, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (55705, '<', 'Less than 55705 + Jackpot', '', 85.000, 1.147, 2, 8750, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (52429, '<', 'Less than 52429 + Jackpot', '', 80.000, 1.219, 2, 8200, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (49152, '<', 'Less than 49152 + Jackpot', '', 75.000, 1.300, 2, 7500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (45875, '<', 'Less than 45875 + Jackpot', '', 70.000, 1.393, 2, 7000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (42598, '<', 'Less than 42598 + Jackpot', '', 65.000, 1.500, 2, 6500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (39321, '<', 'Less than 39321 + Jackpot', '', 60.000, 1.625, 2, 6000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (36045, '<', 'Less than 36045 + Jackpot', '', 55.000, 1.773, 2, 5500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (32768, '<', 'Less than 32768 + Jackpot', '', 50.000, 1.950, 2, 5000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (29491, '<', 'Less than 29491 + Jackpot', '', 45.000, 2.167, 2, 4500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (26214, '<', 'Less than 26214 + Jackpot', '', 40.000, 2.438, 2, 4000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (22938, '<', 'Less than 22938 + Jackpot', '', 35.000, 2.786, 2, 3500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (19661, '<', 'Less than 19661 + Jackpot', '', 30.000, 3.250, 2, 3000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (16384, '<', 'Less than 16384 + Jackpot', '', 25.000, 3.900, 2, 2500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (13107, '<', 'Less than 13107 + Jackpot', '', 20.000, 4.875, 2, 2000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (9831, '<','Less than 9831 + Jackpot', '', 15.000, 6.500, 2, 1500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (6554, '<', 'Less than 6554 + Jackpot', '', 10.000, 9.750, 2, 1000, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (3277, '<','Less than 3277 + Jackpot', '', 5.000, 19.500, 2, 500, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (1639, '<','Less than 1639 + Jackpot', '', 2.500, 39.000, 2, 250, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (1311, '<','Less than 1311 + Jackpot', '', 2.000, 48.750, 2, 200, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (984, '<','Less than 984 + Jackpot', '', 1.500, 65.000, 2, 150, 1);
INSERT INTO dicedb.available_bets (game_value, game_operator, game_desc, coin_address, winning_odds, win_multiplier, min_bet, max_bet, jackpot_bet) 
VALUES (656, '<','Less than 656 + Jackpot', '', 1.000, 97.500, 2, 100, 1);

--
-- Table that contains secrets used to calculate the lucky numbers
--
CREATE TABLE dicedb.secrets
(
  id int(255) NOT NULL AUTO_INCREMENT,
  secret_hash varchar(32) NOT NULL,
  valid_from timestamp NOT NULL,
  valid_to timestamp NULL DEFAULT NULL,
  CONSTRAINT SCS_PK PRIMARY KEY (id)
)
COMMENT = 'Table that contains the secret hashes to calculate the lucky numbers';

--
-- Table that contains the bets placed by users and their results
--
CREATE TABLE dicedb.placed_bets
(
  id int(255) NOT NULL AUTO_INCREMENT,
  abs_id int(255) NOT NULL,
  scs_id int(255) NOT NULL,
  bet_time timestamp NOT NULL,
  bet_value decimal(20,8) NOT NULL,
  sender_coin_address char(255) NOT NULL,
  tx_id char(255) NOT NULL,
  confirmations int(10) NULL,
  game_value int(10) NOT NULL,
  lucky_hash varchar(510) NOT NULL,
  lucky_number int(10) NOT NULL,
  bet_result tinyint(1) NOT NULL,
  bet_error varchar(2000) DEFAULT NULL,
  refunded tinyint(1) NOT NULL DEFAULT '0',
  jackpot_bet tinyint(1) NOT NULL DEFAULT '0',
  jackpot_result tinyint(1) NOT NULL DEFAULT '0',
  jackpot_value decimal(20,8) NULL,
  jackpot_payout_tx_id varchar(255) DEFAULT NULL,
  payout decimal(20,8) NOT NULL,
  payout_executed tinyint(1) NOT NULL DEFAULT '0',
  payout_executed_time timestamp NULL DEFAULT NULL,
  payout_tx_id varchar(255) DEFAULT NULL,
  CONSTRAINT PBS_PK PRIMARY KEY (id),
  INDEX PBS_INDEX_1 (tx_id),
  INDEX PBS_INDEX_2 (bet_time),
  INDEX PBS_INDEX_3 (confirmations)
)
COMMENT = 'Table that contains all placed bets by users and their results';

--
-- Table that contains the bet transactions
--
CREATE TABLE dicedb.transactions (
  id int(255) NOT NULL AUTO_INCREMENT,
  tx_type varchar(25) DEFAULT NULL,
  tx_id varchar(255) DEFAULT NULL,
  tx_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  coin_address varchar(255) DEFAULT NULL,
  amount decimal(20,8) DEFAULT '0.00000000',
  confirmations int(10) DEFAULT NULL,
  blockhash varchar(65) DEFAULT NULL,
  blockheight int(10) DEFAULT NULL,
  account_id int(255) DEFAULT NULL,
  comments text,
  archived tinyint(1) NOT NULL DEFAULT '0',
  executed tinyint(1) NOT NULL DEFAULT '0',
  executed_time timestamp NULL DEFAULT NULL,
  CONSTRAINT TTS_PK PRIMARY KEY (id),
  UNIQUE KEY TTS_TX_ID_UK (tx_id),
  INDEX TTS_INDEX_1 (confirmations),
  INDEX TTS_INDEX_2 (tx_time),
  INDEX TTS_INDEX_3 (account_id),
  INDEX TTS_INDEX_4 (executed)
)
COMMENT = 'Table that contains all wallet transactions';