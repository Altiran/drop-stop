# DropStop

A highly customizable plugin that can be used to disable the ability of dropping items by players and warn them to not
drop items again. The plugin is fully customizable with a configuration file and supports BungeeCord and Velocity.

[![Build & Test](https://github.com/Altiran/drop-stop/actions/workflows/main.yml/badge.svg)](https://github.com/Altiran/drop-stop/actions/workflows/main.yml)
[![WakaTime](https://wakatime.com/badge/github/Altiran/drop-stop.svg)](https://wakatime.com/badge/github/Altiran/drop-stop)

**Like the project? Make sure to leave a ⭐ on the repository!**

## Features

- Prevent players from dropping items.
- Warn players to not drop items again.
- Fully customizable with a configuration file.
- Item allowlisting to allow dropping for certain items.
- Warn players with a custom message.
- Absolutely free and open-source.
- Supports BungeeCord and Velocity.
- Latest version 1.21, including snapshots.

## Installation

There are a few ways to install the plugin on your server:

1. **PaperMC**: You can download the plugin from the PaperMC
   page [here](https://hangar.papermc.io/Altiran/DropStop).
2. **SpigotMC**: You can download the plugin from the SpigotMC
   page [here](https://www.spigotmc.org/resources/antiitemdrop.116889/).
3. **GitHub Releases**: You can download the latest version of the plugin from the Releases
   page [here](https://github.com/Altiran/drop-stop/releases/latest).
4. **Build from Source**: You can build the plugin from the source code by cloning the repository and running
   the `./gradlew build` command.

To install the plugin, drag and drop the downloaded JAR file into the `plugins` folder of your server folder.
Restart the server to load the plugin.

## Configuration

You can customize the settings of the plugin by changing the values in the configuration file. The configuration file is
automatically generated when the plugin is loaded for the first time. It will be located
at `YourServerFolder/plugins/DropStop/config.yml`. Here is the default configuration file:

```yaml
# ************************* #
#   DropStop Configuration  #
# ************************* #

# DISABLE ITEM DROPS
# Enables the core feature of the plugin to disable item drops.
# When enabled, players will not be able to drop items.
# Default: true
disable-item-drops: true

# WARN PLAYER ON DROP
# Enable this to warn players if they try to drop items when the plugin is enabled.
# Default: false
warn-player-on-drop: false

# WARNING MESSAGE
# Give a message to send as a warning when dropping items (only works when 'warn-player-on-drop' in enabled).
# Use & for Minecraft text formatting and %player% for the player name.
# Default: "&6&lHey %player%, you are not allowed to drop that here."
warning-message: "&6&lHey %player%, you are not allowed to drop that here."

# WARNING TIMEOUT
# Time in seconds to wait before sending the warning message again.
# This feature is to prevent spamming the warning message.
# Set to 0 to disable the feature.
# Default: 5
warning-timeout: 5

# ITEM ALLOWLISTING
# Enable item allowlist to allow dropping for certain items and restrict for the rest.
# When enabled, only the items in the list will be allowed to drop.
# Default: false
item-allowlisting: false

# ITEM ALLOWLIST
# This is the list of items which can be dropped even when the plugin is enabled.
# Can hold different item names e.g., DIAMOND, STICK, DIRT.
# Default:
#    - DIAMOND
#    - GOLD_BLOCK
item-allowlist:
    - DIAMOND
    - GOLD_BLOCK
```

<!-- MADE WITH ❤️ BY ALTIRAN -->
