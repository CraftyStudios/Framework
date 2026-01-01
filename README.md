# Getting Started

## Dependency Setup

Repository:
```kotlin
maven("https://repo.craftystudios.net/repository/maven-public/") { 
    name = "crafty-repo" 
}
```

Dependency:
```kotlin
compileOnly("dev.crafty:framework:<version>")
```

Replace `<version>` with the latest version available on the [Crafty Studios Maven Repository](https://repo.craftystudios.net/repository/maven-public/).

> [!WARNING]
> Be sure to set your kotlin stdlib to `compileOnly`, as the framework already includes it.

## Main Class
Your main class must extend `dev.crafty.framework.api.lifecycle.FrameworkPlugin`
```kotlin
class MyPlugin : FrameworkPlugin("My Framework Plugin") {
    override fun initialize() {
        
    }

    override fun shutdown() {

    }
}
```

NOTE: You must create a `lang.yml` in resources for the plugin to load properly, even if you don't use any i18n features.

# Features
The framework provides a variety of features to help you build your plugin:
- [Dependency Injection](#dependency-injection)
- [Data Management](#data-management)
- [Event Registration](#event-registration)
- [i18n Support](#i18n-support)
- [Logging Utilities](#logging-utilities)
- [Task Scheduling](#task-scheduling)
- [Menu System](#menu-system)
- [Config System](#config-system)
- [Command Framework](#command-framework)

# Dependency Injection
The core of the framework is built around a powerful dependency injection system. You can easily inject services and components into your classes using [Koin](https://insert-koin.io/).
```kotlin
class MyPlugin : FrameworkPlugin("My Framework Plugin"), KoinComponent {
    private val logger: Logger by inject()
    
    override fun initialize() {
        logger.info("Hello world")
    }

    override fun shutdown() {

    }
}
```

NOTE: Be sure to implement `KoinComponent` in any class you wish to use dependency injection in.
The import for `inject` is `import org.koin.core.component.inject`. If you do not implement KoinComponent, IntelliJ (and potentially other IDEs) will not see this import and will suggest another one. 
This will result in a compile error.

### Registering Modules
You can register your own Koin modules in your main class's `initialize` method:
```kotlin
class MyPlugin : FrameworkPlugin("My Framework Plugin"), KoinComponent {
    private val logger: Logger by inject()
    
    override fun initialize() {
        logger.info("Hello world")

        // loadKoinModules is a function provided by Koin
        loadKoinModules(module {
            single<MyService> { MyServiceImpl() }
        })
    }

    override fun shutdown() {

    }
}
```

In this example, `MyService` is an interface defining the api contract, and `MyServiceImpl` is the concrete implementation.
You can then inject `MyService` wherever you need it. This will work cross-plugin if you expose the api interface, as `loadKoinModules` hooks into the main Koin context used by the framework.

# Data Management
First, make sure your plugin has a `CoroutineScope` defined.
```kotlin
class MyPlugin : FrameworkPlugin("My Framework Plugin"), KoinComponent {
    private val logger: Logger by inject()

    companion object {
        lateinit var instance: FrameworkTest
    }

    lateinit var scope: CoroutineScope

    override fun initialize() {
        instance = this
        
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // loadKoinModules is a function provided by Koin
        loadKoinModules(module {
            single<MyService> { MyServiceImpl() }
        })

        logger.info("Hello world")
    }

    override fun shutdown() {

    }
}
```

```kotlin
// keys/DataKeys.kt
fun userSettingKey(player: Player) = key<Boolean>("")

// MyServiceImpl.kt
class MyServiceImpl : KoinComponent {
    private val data: DataStore by inject()
    
    fun demonstrateGetting(player: Player) {
        MyPlugin.instance.scope.launch {
            val setting = data.get(
                userSettingKey(player)
            ) // mutating this variable will not persist the change, this is a snapshot
            
            // type of setting is Boolean?
            
            // do something... 
        }
    }
    
    fun demonstrateSetting(player: Player) {
        // we are just going to toggle it
        MyPlugin.instance.scope.launch {
            data.transaction {
                // look at dev.crafty.framework.api.data.Transaction for methods you can use in the transaction block
                
                update(userSettingKey(player)) {
                    it?.not() ?: true // if null, set to true
                }
            } 
            // changes are atomic, automatic rollback on exception
        }
    }
}
```

The Data Store system is already async and Mutex guarded, so you don't need to worry about thread safety or blocking the main thread.

# Event Registration
```kotlin
val eventId = on<PlayerJoinEvent> { event, id ->
    GlobalEventRouter.unregisterListener(id) // if you want to unregister within the handler
}

// or after 
```

NOTE: Only supports default Bukkit events for now, will add support for scanning custom events in the future.

# i18n Support
```kotlin
// keys/I18nKeys.kt
object PrefixKey : I18nKey {
    override val path: String = "prefix"
}

object TestKey : I18nKey {
    override val path: String = "test"
}

// MyPlugin.kt
class MyPlugin : FrameworkPlugin("My Framework Plugin"), KoinComponent {
    private val logger: Logger by inject()
    private val i18n: I18n by inject()

    companion object {
        lateinit var instance: FrameworkTest
    }

    lateinit var scope: CoroutineScope

    override fun initialize() {
        instance = this

        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        i18n.setPluginPrefix(
            i18n.getRaw(PrefixKey)
        ) // i18n will automatically scan a trace to find the calling plugin

        // loadKoinModules is a function provided by Koin
        loadKoinModules(module {
            single<MyService> { MyServiceImpl() }
        })

        on<PlayerJoinEvent> { event, _ ->
            val player = event.player
            i18n.send(player, TestKey, "player" to player.name) // do not include {} when passing variables
        }

        logger.info("Hello world")
    }

    override fun shutdown() {

    }
}
```

```yaml
# resources/lang.yml
prefix: "<green>[MyPlugin] "
test: "<red>Hello, {player}! Welcome to the server."
```

NOTE: MiniMessage is the only supported format for i18n messages.

# Logging Utilities
The framework provides a logger that is pre-configured with your plugin's name.
Look at `dev.crafty.framework.api.logs.Logger` for available methods.

# Task Scheduling
The framework provides some utilities for scheduling tasks with the Bukkit scheduler.
```kotlin
every(5.ticks, delay = 10.ticks) {
    // runs every 5 ticks
}

every(5.ticks, async = true) {
    // runs every 5 ticks async
}

later(5.minutes) {
    // runs once after 5 minutes
}

later(5.minutes, async = true) {
    // runs once after 5 minutes async
}

now {
    // runs on the next tick on the main thread
}

now(async = true) {
    // runs on the next tick async
}
```

All task methods return a `BukkitTask` which can be used to cancel the task if needed.

# Menu System
The framework provides a menu system to create interactive inventories.

### Static Menus

```kotlin
class TestStaticMenu(player: Player) : Menu(player) {
    override val owningPlugin: MyPlugin
        get() = MyPlugin.instance

    override val id: String
        get() = "test-static-menu"

    override fun placeholders(): Map<String, Any> = mapOf(
        "player_name" to player.name
    )

    @ClickAction(id = "special-action")
    fun onSpecialAction(event: InventoryClickEvent) { // must have InventoryClickEvent parameter
        player.sendMessage("You clicked the special action item! Slot: ${event.slot}")
    }
}
```

Now, you must create a yml file corresponding to the id in the resources/menus directory.

```yaml
# resources/menus/test-static-menu.yml
title: "<red>Test Menu"
rows: 6
type: CHEST

pattern:
  - "XXXXXXXXX"
  - "X-------X"
  - "X-------X"
  - "X-------X"
  - "X-------X"
  - "XXXXXXXXX"

items:
  'X':
    material: "BLACK_STAINED_GLASS_PANE"
    name: " "
    lore: []
  '-':
    material: "DIAMOND"
    name: "<green>Special Diamond"
    lore:
      - "<yellow>This is a special diamond item."
      - "<yellow>It has unique properties."
    enchantments:
      - "UNBREAKING:1"
    flags:
      - "HIDE_ENCHANTS"
      - "HIDE_ATTRIBUTES"
    actions:
      left-click:
        - "special-action"
```

### Paginated Menus
```kotlin
class TestPaginatedMenu(player: Player) : PaginatedMenu<String>(player) {
    override suspend fun data(): List<String> {
        // suspend allows you to fetch from the data system WITHOUT launching a new coroutine
        return List(50) { index -> "Item #${index + 1}" }
    }

    override fun paginatedPlaceholders(): Map<String, (String) -> Any> {
        return mapOf(
            "item_name" to { item: String -> item }
        )
    }

    // you can attach a persistent data container to an item if you want to identify it later (such as in a click action)
    override fun materialProviders(): Map<String, (String) -> ItemStack> {
        return mapOf(
            "item_material" to { _: String -> ItemStack(Material.DIAMOND) }
        )
    }

    override fun staticPlaceholders(): Map<String, Any> {
        return mapOf(
            "player_name" to player.name
        )
    }

    @ClickAction(id = "special-action")
    fun onSpecialAction(event: InventoryClickEvent) {
        player.sendMessage("You clicked the special action item! Slot: ${event.slot}")
    }

    override val owningPlugin: FrameworkPlugin
        get() = FrameworkTest.instance

    override val id: String
        get() = "test-paginated-menu"
}
```

Now, you must create a yml file corresponding to the id in the resources/menus directory.

```yaml
# resources/menus/test-paginated-menu.yml
title: "<red>Test Menu"
rows: 6
type: CHEST

pattern:
  - "XXXXXXXXX"
  - "X-------X"
  - "X-------X"
  - "X-------X"
  - "X-------X"
  - "pXXXXXXXn"

items:
  'X':
    material: "BLACK_STAINED_GLASS_PANE"
    name: " "
    lore: []
  '-':
    name: "{item_name}"
    lore:
      - "<yellow>Hello {player_name}."
    paginated-options:
      is-paginated: true
      provide-material: true
      material-key: "item_material"
    enchantments:
      - "UNBREAKING:1"
    flags:
      - "HIDE_ENCHANTS"
      - "HIDE_ATTRIBUTES"
    actions:
      left-click:
        - "special-action"
  'p':
    material: "ARROW"
    name: "<green>Previous Page"
    lore:
      - "<yellow>Go to the previous page."
    actions:
      left-click:
        - "previous-page"
  'n':
    material: "ARROW"
    name: "<green>Next Page"
    lore:
      - "<yellow>Go to the next page."
    actions:
      left-click:
        - "next-page"
```

# Config System
First define the annotated data class:
```kotlin
@KtConfig(hasDefault = true)
data class DataConfig(
    @Comment("PostgreSQL database configuration.")
    var postgres: PostgresConfig = PostgresConfig(),
)

@KtConfig(hasDefault = true)
data class PostgresConfig(
    @Comment("The hostname or IP address of the PostgreSQL server.")
    var host: String = "localhost",

    @Comment("The port of the PostgreSQL server.")
    var port: Int = 5432,

    @Comment("The database name of the PostgreSQL server.")
    var database: String = "mydatabase",

    @Comment("The username for the PostgreSQL database.")
    var username: String = "myuser",

    @Comment("The password for the PostgreSQL database.")
    var password: String = "mypassword"
) {
    fun toJdbcUrl(): String {
        return "jdbc:postgresql://$host:$port/$database"
    }
}
```
NOTE: You should NOT create the yml file in resources. Our setupConfig() method handles this (see below). 

Next, run a `gradle build` to have the config library generate the loader

Add the following to the initialize method of your plugin:

```kotlin
override fun initialize() {
    // DataConfigLoader is the generated object from the config library
    val configModule = setupConfig("data.yml", DataConfigLoader, DataConfig(), this)

    loadKoinModules(module {
        single<BlockworksConfig> { configModule }
        // add more here
    })
}
```

Now you can inject your config anywhere using the normal inject pattern.
```kotlin
val dataConfig: DataConfig by inject()
```

We use [ktConfig](https://github.com/sya-ri/ktConfig) to back the config system. Refer to their documentation for a detailed api reference.

# Command Framework
We decided to not reinvent the wheel and instead have our plugins use [Aikar's Command Framework (ACF)](https://github.com/aikar/commands/wiki/Using-ACF).
Please refer to their wiki for documentation on how to use ACF.
