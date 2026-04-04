ALTER TABLE wallets ADD COLUMN last_ad_reward_date DATE;
ALTER TABLE wallets ADD COLUMN daily_ad_reward_count INT NOT NULL DEFAULT 0;
