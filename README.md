# Party Dungeons

## Information
`PartyDungeons` is a Minecraft Spigot plugin aimed to bring builds to life by managing dungeon instances.
This plugin will provide a framework for a dungeon, such as it's party and states while the actual dungeon is
powered through scripts.
`PartyDungeons` uses the Nashorn script engine and JavaScript to run scripts.

## Download Process
There are two versions of downloadable JAR files:
- `PartyDungeons.jar`
- `PartyDungeons-shaded.jar`

**`PartyDungeons` requires the use of Nashorn to be used.**<br>
Nashorn was deprecated on JDK 11. If your current JDK version does not include Nashorn you ***MUST*** download the
`shaded` version of the `PartyDungeons` plugin.

## Dungeon Setup Process
**Dungeon setup and modification requires the `partydungeons.admin` permission.**
1. Run `/partydungeons createdungeon <dungeon_name>`. **This process will require you to select an area**,
so look/target the first corner block of your dungeon area.
2. Set your dungeon's spawn position with `/partydungeons setspawnposition <dungeon_name>`. This is the location where
players will be teleported when they leave the dungeon or disconnect while the dungeon is active.
3. Set your dungeon's start position with `/partydungeons setstartposition <dungeon_name>`. This is the location where
all party members will be teleported when the dungeon begins.
4. Begin scripting your dungeon.
  - Create scripts with `/partydungeons createscript <dungeon_name> <script_type>` and edit the files.
  - Download existing scripts from a file manifest with `/partydungeons download <dungeon_name> <manifest_url>`.
5. Your dungeon setup should now be complete!

## Commands
**All commands are assumed to be prefixed with `/partydungeons`**
| Command | Description | Example |
| :---: | :--- | :---: |
| `help` | Get `PartyDungeons`' help menu. | `help` |
| `join` | Join a dungeon you are in the area of. | `join` |
| `leave` | Leave a dungeon you are a part of. | `leave` |
| `status` | Get the dungeon's current status.<br>You must be in the party of the dungeon to get this information. | `status` |
| `createdungeon` | Admin command.<br>Create a new dungeon. | `createdungeon <dungeon_name>` |
| `createworlddirectory` | Admin command.<br>Create a new world directory and all necessary files. | `createworlddirectory` |
| `loaddungeon` | Admin command.<br>Load an unloaded dungeon. | `loaddungeon <dungeon_name>` |
| `unloaddungeon` | Admin command.<br>Unload a loaded dungeon.<br>Players participating in this dungeon will be reset and teleported out. | `unload dungeon <dungeon_name>` |
| `setspawnposition` | Admin command.<br>Set the SPAWN position of a dungeon.<br>A spawn position is where users will be teleported when leaving. | `setspawnposition <dungeon_name>` |
| `setstartposition` | Admin command.<br>Set the START position of a dungeon.<br>A start position is where users will be teleported when the dungeon begins. | `setstartposition <dungeon_name>` |
| `createscript` | Admin command.<br>Create and save a new script file.<br>Script boilerplate code will be included with this method. | `createscript <dungeon_name> <script_type>` |
| `createworldscript` | Admin command.<br>Create and save a new WORLD script file.<br>Script boilerplate code will be included with this method. | `createscript <script_type>` |
| `deletescript` | Admin command.<br>Delete a script in the given file path.<br>`plugins/PartyDungeons/<file_path>` will be deleted. Only `.js` files can be deleted with this method. | `deletescript <file_path>` |
| `settings` | Admin command.<br>Change a dungeon's `max_party` and `daily_clear` values. | `settings <dungeon_name> <setting_type> [value]` |
| `runscript` | Admin command.<br>Run a script as if you triggered it normally.<br>Try to avoid using this command. | `runscript <dungeon_name> <script_type> <script_name>` |
| `download` | Admin command.<br>Read from the provided file manifest and bulk download files.<br>Including `<dungeon_name>`: Download to `plugins/PartyDungeons/dungeon/<dungeon_name>`<br>No `<dungeon_name>`: Download to `plugins/PartyDungeons`<br>All existing files will be overwritten, so don't use this command if you fear overwriting important files. | `download <dungeon_name> <manifest_url>`<br>`download <manifest_url>` |
| `manifest` | Admin command.<br>Generate a file manifest for the dungeon.<br>The generated manifest must be reviewed before it is used. | `manifest <dungeon_name> [root_url]` |

## Script Types
There are four different ways scripts can be triggered:<br>
| Script Type | Description |
| :---: | :--- |
| `Area Walk` | Triggers when a player is in a defined bounding box.<br>Code can be triggered when the player enters, exits, or every block the player walks around in the area. |
| `Dungeon` | Fires only during specific dungeon events, look at `Dungeon Script Type Scripts` for more information. |
| `Interact` | Triggers when a player left or right clicks a block. |
| `Walk` | Triggers when a player walks over the block location. |

**Most script types will and should only be triggered when the player is in a dungeon party.**

## Dungeon Script Type Scripts
Dungeon scripts are scripts that will be triggered during specific events.<br>
You can find these scripts in the `PartyDungeons/dungeon/<dungeon_name>/scripts/dungeon` directory.
| Script | Description |
| :---: | :--- |
| `DungeonStatus` | Changes the behavior of `/partydungeons status` |
| `onDungeonReset` | Called when the dungeon should be reset. Perform world editing or entity clearing here. |
| `onEntityDeath` | Called when an entity in the dungeon area dies. |
| `onPartyMemberJoin` | Called when a new player joins the party. Handle how the dungeon should be started here. |
| `onPartyMemberQuit` | Called when a player leaves the party while the dungeon is in progress. Handle if the dungeon should stop here. |
| `onPlayerRebirth` | Changes the behavior of `/partydungeons join` when a dead player tries to rejoin. |
| `onPlayerReset` | Called when a player quits a dungeon session. Handle removing permanent effects from the player here. |
| `onPlayerRespawn` | Called when a player respawns after dying. Handle if you should teleport the player back somewhere here. |
| `onPlayerDeath` | Called when a player dies in a dungeon. Handle what should happen here. |

## Dungeon Scripting Tips
- Performing tasks like teleporting players, placing blocks, or spawning entites requires using `BukkitScheduler`. The script can not perform these tasks directly because scripts run in a seperate thread.
- When your dungeon is "cleared" you must call `dungeon.clear()` from your script. This will make sure players can safely leave and that the daily clear counter will work.
- Use `None` type scripts to hold reusable components and load those components with Nashorn's `load()` function.
- Depending on the script type, they may have different script bindings. Please review the auto generated comment block in your script to see what objects your script most likely has access to.

## Dungeon Scripts VS World Scripts
As of `v1.1`, World Scripts have been added.

World Scripts are scripts that can be triggered by ***anyone that's not in a dungeon party***. *Players in a dungeon instance will not be able to trigger these scripts*. World scripts are to be used if you want the scripting system of `PartyDungeons` to power different non-dungeon instances.

Despite having this feature, **`PartyDungeons` will always be a dungeon instance manager**, so *flexibility of World Scripts may not be up to standards with other scripting plugins*.

## FastAsyncWorldEdit Support
As of `v1.2`, `FastAsyncWorldEdit` has been added to `PartyDungeons`, which opens up some new possibilities.<br>
The example code below will work without the need of `BukkitScheduler`, thanks to `FastAsyncWorldEdit`'s implementation.

**Schematic Pasting Example:**
Assuming you already have a schematic saved via `//schem save <schematic_name>`
```javascript
const Location = Java.type("org.bukkit.Location");
const file_path = `${sm.getDungeonDirectory("<dungeon_name>")}/<schematic_name>.schem`;
const location = new Location(player.getWorld(), x, y, z);
sm.pasteSchematic(file_path, location);
```

**Block NBT Editing:**
This is useful for modifying NBT data not possible with Spigot.<br>
Overall, NBT editing can get pretty confusing, so try to avoid doing so if you can.<br>
Creating your desired block with commands first is recommended to see how NBT is formatted with `FastAsyncWorldEdit`'s `//nbtinfo` command.

*Changing Basic Pig Spawner to Zombie with NBT Data*
```javascript
// Assuming we're modifying the NBT of a CreatureSpawner at [x, y, z]...
const Location = Java.type("org.bukkit.Location");
const location = new Location(player.getWorld(), x, y, z);
let nbt = sm.getNBT(location);

// Modify SpawnData (next creature to be summoned)
let spawn_data = nbt.get("SpawnData");
spawn_data = spawn_data.putString("id", "minecraft:zombie");
spawn_data = spawn_data.putBoolean("NoAI", true);
spawn_data = spawn_data.putString("CustomName", "\"Cool Name\"");

// Add potion effects to creature
let active_effects_list = spawn_data.getList("ActiveEffects");
let effect = nbt.getCompound("newCompound");
effect = effect.put("Id", 1); // SPEED
effect = effect.put("Amplifier", 0);
effect = effect.put("Duration", 999999);
active_effects_list = active_effects_list["add(BinaryTag)"](effect);
spawn_data = spawn_data.put("ActiveEffects", active_effects_list);

// Set the next creature to spawn from our CreatureSpawner to spawn_data
nbt = nbt.put("SpawnData", spawn_data);

// Modify SpawnPotentials (Replace the only existing one)
// WE NEED TO MODIFY THIS OTHERWISE OUR spawn_data WILL NEVER
// SPAWN AGAIN AFTER THE FIRST SPAWN
let spawn_potentials_list = nbt.getList("SpawnPotentials");
let list_entry = list.get(0);
list_entry = list_entry.put("Entity", spawn_data);
spawn_potentials_list = list.set(0, list_entry, null);
nbt = nbt.put("SpawnPotentials", list);

// Save NBT to our CreatureSpawner
sm.setNBT(location, nbt);
```

*Adding NBT Data to a CreatureSpawner with .setSpawnedType(EntityType.FALLING_BLOCK)*
```javascript
// Assuming we're modifying the NBT of a CreatureSpawner at [x, y, z]...
const Location = Java.type("org.bukkit.Location");
const location = new Location(player.getWorld(), x, y, z);
let nbt = sm.getNBT(location);

// SPAWNER SETTINGS
nbt = nbt.putShort("MaxNearbyEntities", 31);
nbt = nbt.putShort("RequiredPlayerRange", 50);
nbt = nbt.putShort("SpawnCount", 30);
nbt = nbt.putShort("Delay", 1);
nbt = nbt.putShort("MaxSpawnDelay", 1);
nbt = nbt.putShort("MinSpawnDelay", 1);

// SPAWN DATA
let spawn_data = nbt.get("SpawnData");
spawn_data = spawn_data.putString("id", "minecraft:falling_block");
spawn_data = spawn_data.putInt("Time", 1);
spawn_data = spawn_data.putBoolean("DropItem", false);
spawn_data = spawn_data.putBoolean("HurtEntities", true);
spawn_data = spawn_data.putInt("FallHurtMax", 2);
spawn_data = spawn_data.putFloat("FallHurtAmount", 2.0);

// SPAWN DATA : BLOCK STATE
let block_state = nbt.getCompound("newBlockState");
block_state = block_state.putString("Name", "minecraft:pointed_dripstone");
let properties = nbt.getCompound("newProperties");
properties = properties.putString("thickness", "tip_merge");
properties = properties.putString("vertical_direction", "down");
properties = properties.putString("waterlogged", "false");
block_state = block_state.put("Properties", properties);
spawn_data = spawn_data.put("BlockState", block_state);

// SPAWN DATA : SAVE TO NBT
nbt = nbt = nbt.put("SpawnData", spawn_data);

// SPAWN POTENTIALS
let list = nbt.getList("SpawnPotentials");
let entry = list.get(0);
entry = entry.put("Entity", spawn_data);
list = list.set(0, entry, null);
nbt = nbt.put("SpawnPotentials", list);

sm.setNBT(location, nbt);
```

## Example Scripts
Example scripts can be found here: <https://github.com/Expugn/PartyDungeonsScripts><br>
These scripts are intended to be used as an example or reference when writing your own rather than to be plug-and-played.

## Checkstyle
This code follows the checkstyle rules from `com/github/ngeor/checkstyle.xml`.<br>
JAR compliation will not pass until all checkstyle rules are passing.<br>
If this is found to be annoying:
- Open `pom.xml`
- Change `<skipTests>false</skipTests>` to `<skipTests>true</skipTests>`

## Other Stuff
**Project** began on October 9, 2021<br>