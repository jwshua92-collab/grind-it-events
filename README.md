# Grind It Events

A RuneLite plugin for members of the Grind It clan. During our clan events, it sends the drops that count for your team to the event site at bingo.grinditosrs.com, each with a screenshot. It also shows the event password on screen, so your screenshots prove when they were taken.

## Setting it up

1. Install **Grind It Events** from the Plugin Hub.
2. Sign in at [bingo.grinditosrs.com/plugin](https://bingo.grinditosrs.com/plugin) and get your link code.
3. Open the plugin's settings in RuneLite and paste the code into **Link code**. Keep it to yourself: it's how the site knows the drops are yours.
4. In the game, turn on **Settings → Chat → Collection log - New addition notification**, so new collection log slots are picked up.

When you log in, a message in your game chat confirms what the plugin is watching for. After each drop it sends, the site's answer appears in chat, e.g. *Submitted Draconic visage for Vorkath heads (waiting for review)*.

## Settings

| Setting | What it does |
| --- | --- |
| Link code | Your personal code from the plugin page. Nothing is sent until it's set. |
| Show event password | Shows the event name, password and time (UTC) while an event you're in is running. |
| Show date and time | The time line under the password. |
| Chat messages | Says in your game chat what happened to each drop. |

## What the plugin sends

Nothing at all until you add a link code. With one set, it only talks to bingo.grinditosrs.com:

- **When you log in, and every few minutes after:** your RSN, to get the current event, your team, the items to watch for and the event password.
- **When you get a drop the event is looking for:** your RSN, the item, the quantity, what dropped it, your kill count, and a screenshot of the game window.

Drops that aren't on the event's board are never sent, and nothing else leaves your client.

## Credits

The chat messages the plugin recognises (kill counts, collection log slots, pets) follow the game's wording and [Dink](https://github.com/pajlads/DinkPlugin)'s handling of it (BSD 2-Clause).

## Licence

BSD 2-Clause, see [LICENSE](LICENSE).
