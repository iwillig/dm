-- Create species enumeration table
CREATE TABLE species (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  size TEXT,
  speed INTEGER,
  special_traits TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- ;;

-- Create classes enumeration table
CREATE TABLE classes (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  hit_die INTEGER,
  primary_ability TEXT,
  saving_throw_proficiencies TEXT,
  armor_proficiencies TEXT,
  weapon_proficiencies TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- ;;

-- Create attribute_names enumeration table
CREATE TABLE attribute_names (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  abbreviation TEXT,
  description TEXT,
  display_order INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- ;;

-- Create skills enumeration table
CREATE TABLE skills (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  attribute_code TEXT NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (attribute_code) REFERENCES attribute_names(code)
);
-- ;;

-- Create characters table
CREATE TABLE characters (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  species_code TEXT NOT NULL,
  class_code TEXT NOT NULL,
  armor_class INTEGER,
  inspiration BOOLEAN DEFAULT FALSE,
  level INTEGER DEFAULT 1,
  hit_points_max INTEGER,
  hit_points_current INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (species_code) REFERENCES species(code),
  FOREIGN KEY (class_code) REFERENCES classes(code)
);
-- ;;

-- Create character_attributes table
CREATE TABLE character_attributes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  character_id INTEGER NOT NULL,
  attribute_code TEXT NOT NULL,
  attribute_value INTEGER NOT NULL CHECK(attribute_value >= 0 AND attribute_value <= 30),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
  FOREIGN KEY (attribute_code) REFERENCES attribute_names(code),
  UNIQUE(character_id, attribute_code)
);
-- ;;

-- Create character_skills table
CREATE TABLE character_skills (
  character_id INTEGER NOT NULL,
  skill_code TEXT NOT NULL,
  proficiency_level INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (character_id, skill_code),
  FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
  FOREIGN KEY (skill_code) REFERENCES skills(code)
);
-- ;;

-- Create items table
CREATE TABLE items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  description TEXT,
  item_type TEXT,
  quantity INTEGER DEFAULT 1,
  weight REAL,
  value_copper INTEGER,
  properties TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
