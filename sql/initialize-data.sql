USE easycal;

# Food Items
INSERT INTO food_item VALUES ('01009', 'Cheese, cheddar', 403, 3.37, 33.31, 22.87, 0, 0.48, 653);
INSERT INTO food_item VALUES ('45094321', 'Grilled Chicken Strips', 141, 5.88, 3.53, 23.53, 2.4, 0, 412);

# Serving Sizes
INSERT INTO serving_labels (label_value) VALUES
  ('cup, diced'), ('cup, melted'), ('cup, shredded'), ('oz'), ('cubic inch'), ('slice (1 oz)'), ('strips | about');

INSERT INTO serving_size (food_item_id, label_id, ratio) VALUES
  ('01009', 1, 1.32), ('01009', 2, 2.44), ('01009', 3, 1.13), ('01009', 4, 0.28),
  ('01009', 5, 0.17), ('01009', 6, 0.28), ('45094321', 7, 0.14);

# Users
INSERT INTO users (email_address, password) VALUES
  ('mileshenrichs21@gmail.com', '$2b$10$jLKsxJvOCHAeXu/8IG7qFuoHVt60vSNNtPJG9enurJWeG4sALGMHK'),
  ('foo@bar.com', '$2b$10$jLKsxJvOCHAeXu/8IG7qFuoHVt60vSNNtPJG9enurJWeG4sALGMHK');

# Meal Types
INSERT INTO meal (meal_name) VALUES ('Breakfast'), ('Lunch'), ('Dinner'), ('Snacks');

# Consumptions
INSERT INTO consumption (user_id, food_item_id, serving_size_id, serving_quantity, meal, day) VALUES
  (1, '01009', 6, 5, 2, DATE(NOW())),
  (1, '45094321', 7, 3, 2, DATE(NOW()));

# Goals
INSERT INTO goal_categories (category) VALUES
  ('Calories'), ('Carbs'), ('Fat'), ('Protein'), ('Fiber'), ('Sugar'), ('Sodium');

INSERT INTO goals (user_id, goal_category, goal_value) VALUES
  (1, 1, 2200), (1, 2, 260), (1, 3, 86), (1, 4, 100);

# Exercise
INSERT INTO exercise (user_id, calories_burned, day) VALUES (1, 220, DATE(NOW()));