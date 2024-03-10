import java.util.concurrent.Executors
import java.io.*

// Класс, представляющий пользователя
abstract class User(val username: String, val password: String)

// Класс администратора, наследуется от пользователя
class Admin(username: String, password: String) : User(username, password) {
    // Методы для управления меню
    fun addMenuItem(menu: Menu, item: MenuItem) {
        menu.addItem(item)
    }

    fun removeMenuItem(menu: Menu, item: MenuItem) {
        menu.removeItem(item)
    }
}

// Класс посетителя, наследуется от пользователя
class Visitor(username: String, password: String) : User(username, password)

// Класс, представляющий элемент меню
class MenuItem(val name: String, val price: Double, val preparationTime: Int, val complexity: Int)  : Serializable {
    override fun toString(): String {
        return "Блюдо: $name, Цена: $price, Время приготовления: $preparationTime мин, Сложность: $complexity"
    }
}

// Класс, представляющий меню
class Menu : Serializable {
    private var items = mutableListOf<MenuItem>()

    fun addItem(item: MenuItem) {
        items.add(item)
    }

    fun removeItem(item: MenuItem) {
        items.remove(item)
    }

    fun addItems(menuItems: List<MenuItem>) {
        items.addAll(menuItems)
    }

    fun setMenu(menu: Menu){
        items = menu.items
    }

    fun getItems(): List<MenuItem> {
        return items.toList()
    }
}

// Перечисление статусов заказа
enum class OrderStatus {
    NOT_STARTED, PREPARING, READY, DELIVERED, CANCELLED
}

// Класс, представляющий заказ
class Order(val visitor: Visitor, val menu: Menu) {
    private val items = mutableListOf<MenuItem>()
    var status: OrderStatus = OrderStatus.NOT_STARTED
        private set

    fun addItem(item: MenuItem) {
        if (status == OrderStatus.NOT_STARTED || status == OrderStatus.PREPARING) {
            items.add(item)
        } else {
            println("Нельзя добавить блюда в заказ со статусом: $status")
        }
    }

    fun cancelOrder() {
        if (status == OrderStatus.NOT_STARTED || status == OrderStatus.PREPARING) {
            status = OrderStatus.CANCELLED
        } else {
            println("Нельзя отменить заказ со статусом: $status")
        }
    }

    fun processOrder(kitchen: Kitchen, revenue: Double) {
        status = OrderStatus.PREPARING
        kitchen.processOrder(this, revenue )
    }

    fun completeOrder(paymentSystem: PaymentSystem, revenue: Double) {
        status = OrderStatus.READY
        paymentSystem.processPayment(visitor, calculateTotal(), revenue)
    }

    fun calculateTotal(): Double {
        return items.sumOf { it.price }
    }
}

// Класс, представляющий кухню
class Kitchen {
    private val executor = Executors.newFixedThreadPool(5)

    fun processOrder(order: Order, revenue: Double) {
        executor.submit {
            Thread.sleep(order.calculateTotal().toLong()) // Симуляция времени приготовления заказа
            order.completeOrder(PaymentSystem, revenue)
        }
    }
}

// Класс, представляющий систему оплаты
object PaymentSystem {
    fun processPayment(visitor: Visitor, amount: Double, revenue: Double) {
        println("Платеж на сумму $amount успешно прошел ${visitor.username}")
        DataStorage.saveRevenue(revenue)
    }
}

// Класс, представляющий систему аутентификации
object AuthenticationSystem {
    private var users = mutableMapOf<String, User>()

    fun registerUser(user: User) {
        users[user.username] = user
    }

    fun loadUsers(new: Map<String, User>) {
        users = new as MutableMap<String, User>
    }

    fun  getUsers(): MutableMap<String, User> {
        return users
    }

    fun authenticate(username: String, password: String): User? {
        val user = users[username]
        return if (user != null && user.password == password) {
            user
        } else {
            null
        }
    }
}

object DataStorage {
    private const val MENU_FILE_PATH = "menu.txt"
    private const val REVENUE_FILE_PATH = "revenue.txt"
    private const val USERS_FILE_PATH = "users.txt"

    // Метод для сохранения меню
    fun saveMenu(menu: Menu) {
        val file = File(MENU_FILE_PATH)
        BufferedWriter(FileWriter(file)).use { writer ->
            menu.getItems().forEach { menuItem ->
                writer.write("${menuItem.name},${menuItem.price},${menuItem.preparationTime},${menuItem.complexity}")
                writer.newLine()
            }
        }
    }

    // Метод для загрузки меню
    fun loadMenu(): MutableList<MenuItem> {
        val menuItems = mutableListOf<MenuItem>()
        val file = File(MENU_FILE_PATH)

        file.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(",") // Разбиваем строку на части по запятой
                if (parts.size == 4) {
                    val name = parts[0]
                    val price = parts[1].toDouble()
                    val preparationTime = parts[2].toInt()
                    val complexity = parts[3].toInt()
                    val menuItem = MenuItem(name, price, preparationTime, complexity)
                    menuItems.add(menuItem)
                } else {
                    println("Ошибка чтения строки: $line")
                }
            }
        }
        return menuItems
    }

    // Метод для сохранения выручки
    fun saveRevenue(revenue: Double) {
        PrintWriter(REVENUE_FILE_PATH).use { it.println(revenue) }
    }

    // Метод для загрузки выручки
    fun loadRevenue(): Double {
        val file = File(REVENUE_FILE_PATH)
        if (!file.exists()) {
            println("Файл с данными о выручке не найден")
            return 0.0
        }

        val revenueString = file.bufferedReader().readLine()
        if (revenueString != null) {
            return revenueString.toDouble()
        } else {
            println("Файл с данными о выручке пуст")
            return 0.0
        }
    }

    // Метод для сохранения пользователей
    fun saveUsers(users: Map<String, User>) {
        val file = File(USERS_FILE_PATH)
        file.bufferedWriter().use { writer ->
            users.values.forEach { user ->
                writer.write("${user.username},${user.password}") // Записываем данные о пользователе в формате "username,password"
                writer.newLine()
            }
        }
    }

    // Метод для загрузки пользователей
    fun loadUsers(): Map<String, User> {
        val users = mutableMapOf<String, User>()
        val file = File(USERS_FILE_PATH)
        if (!file.exists()) {
            println("Файл с данными о пользователях не найден")
            return emptyMap()
        }

        file.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val (username, password) = line.split(",") // Разбиваем строку на части по запятой
                val user = createUser(username, password) // Создаем пользователя с помощью фабричного метода
                if (user != null) {
                    users[username] = user // Добавляем пользователя в карту
                }
            }
        }
        return users
    }

    private fun createUser(username: String, password: String): User? {
        return when {
            username.startsWith("admin") -> Admin(username, password)
            username.startsWith("customer") -> Visitor(username, password)
            else -> null
        }
    }
}

fun main() {
        while (true) {
            var menuItems = DataStorage.loadMenu()
            var menu = Menu()
            menu.addItems(menuItems)
            var revenue = DataStorage.loadRevenue()
            val users = DataStorage.loadUsers()
            val admin = Admin("admin", "adminpass")
            AuthenticationSystem.loadUsers(users)
            println("Привет! Я система заказов кофейни Кчаю (Кчау)! Как вы хотите войти ?\n Введите:\n1 Если вы Клиент\n2 Если вы админ\n3 Чтобы выключить меня.")
            var open: String = readLine().toString()
            if (open == "1") {
                while (true) {
                    println("Отлично! Введите пожалуйста ваше имя !")
                    var name: String = readLine().toString()
                    var visitor = Visitor(name, "")
                    if (AuthenticationSystem.authenticate(name, "") == null) {
                        AuthenticationSystem.registerUser(visitor)
                    } else {
                        var visitor = AuthenticationSystem.authenticate(name, "")
                    }
                    while (true) {
                        println("Введите:\n1 - Чтобы начать заказ.\n2 - Чтобы выйти из аккаунта.")
                        var visitorsOrder: String = readLine().toString()
                        if (visitorsOrder == "1") {
                            while (true) {
                                val order = Order(visitor, menu)
                                println("Вот ваше меню! Введите:\n Y чтобы посмотреть статус своего заказа\n Z - чтобы добавить блюда в заказ\n O - чтобы отменить заказ\n X - чтобы перейти назад")
                                for (i in menu.getItems()) {
                                    println(i)
                                }
                                var exitMenu: String = readLine().toString()
                                if (exitMenu == "X") {
                                    break;
                                } else if (exitMenu == "Y") {
                                    println(order.status)
                                } else if (exitMenu == "Z") {
                                    while (true) {
                                        println("Я ваш официант, жду номер блюда! Введите X, когда закончите делать заказ.")
                                        var food_number_str: String = readLine().toString()
                                        if (food_number_str == "X"){
                                            break;
                                        }
                                        try {
                                            var food_number = food_number_str?.toInt()
                                            if (food_number != null) {
                                                if (food_number <= menu.getItems().size) {
                                                    order.addItem(menu.getItems()[food_number])
                                                } else {
                                                    println("Введенное число больше чем количество блюд в меню")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            println("Что-то пошло не так")
                                        }
                                    }
                                    order.processOrder(Kitchen(), revenue)
                                } else if (exitMenu == "O") {
                                    order.cancelOrder()
                                }
                            }
                        } else if (visitorsOrder == "2") {
                            break;
                        } else {
                            println("Вероятно вы ввели не то число.")
                        }
                        revenue = DataStorage.loadRevenue()
                    }
                }
            } else if (open == "2") {
                while (true) {
                    println("Отлично! Введите пожалуйста пароль администрации !")
                    var password: String = readLine().toString()
                    if (password == "adminpass") {
                        val admin = Admin("admin", "adminpass")
                        while (true) {
                            println("Введите:\n1 - Чтобы добавить блюдо в меню\n2 - Чтобы удалить\n0 - Чтобы выйти")
                            var adminMenu: String = readLine().toString()
                            if (adminMenu == "1") {
                                while (true) {
                                    println("Введите пожалуйста название блюда или X - Чтобы выйти")
                                    var name: String = readLine().toString()
                                    if(name == "X"){
                                        break;
                                    }
                                    name =  name
                                    while (true){
                                        println("Введите пожалуйста цену блюда")
                                        var price_str: String = readLine().toString()
                                        try{
                                            var price = price_str.toDouble()
                                            while (true) {
                                                 println("Введите время выполнения блюда")
                                                 var preparationTime_str: String = readLine().toString()
                                                 try {
                                                     val preparationTime = preparationTime_str.toInt()
                                                     while(true) {
                                                         println("Введите сложность блюда")
                                                         var complexity_str: String = readLine().toString()
                                                         try {
                                                              val complexity = preparationTime_str.toInt()
                                                              val menuItem = MenuItem(name, price, preparationTime, complexity)
                                                              menu.addItem(menuItem)
                                                              DataStorage.saveMenu(menu)
                                                              menuItems = DataStorage.loadMenu()
                                                              menu = Menu()
                                                              break;
                                                         } catch (e: Exception) {
                                                              println("Некорректный ввод")
                                                              break;
                                                         }
                                                         break;
                                                     }
                                                 } catch (e: Exception) {
                                                      println("Некорректный ввод")
                                                      break;
                                                 }
                                                break;
                                            }
                                        } catch (e: Exception){
                                            println("Некорректный ввод")
                                            break;
                                        }
                                        break;
                                    }
                                }
                            } else if (adminMenu == "2") {
                                while (true) {
                                    println("Вот ваше меню\n Введите порядковый номер блюда, которое вы хотите удалить!\nВведите X -чтобы выйти")
                                    for (i in menu.getItems()) {
                                        println(i)
                                    }
                                    var number_str: String = readLine().toString()
                                    if(number_str == "X"){
                                        break;
                                    }
                                    try{
                                    var number = number_str?.toInt()
                                    var iterator = 0
                                    for (i in menu.getItems()) {
                                        if (iterator == number) {
                                            menu.removeItem(menu.getItems()[iterator])
                                        }
                                        iterator++
                                    }
                                    } catch (e: Exception){
                                        println("Некорректный ввод")
                                    }

                                }
                            } else if (adminMenu == "0") {
                                break;
                            } else {
                                println("Некорректный ввод")
                            }
                        }
                    } else {
                        println("Это не пароль админа")
                    }
                }
            } else if (open == "3") {
                break;
            } else {
                println("Введен некорректный ответ. Сейчас система попытается запросить у вас ввод заново.")
            }
        }
}
