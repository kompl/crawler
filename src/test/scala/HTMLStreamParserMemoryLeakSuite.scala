import cats.effect.IO
import fs2.Stream

import java.nio.charset.StandardCharsets
import com.crawler.common.HTMLStreamParser
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class HTMLStreamParserMemoryLeakSuite extends AnyFunSuite with Matchers {

  // Вспомогательная функция для измерения памяти (приблизительно)
  private def usedMemory: Long = {
    System.gc()
    Thread.sleep(100) // даём время на сборку мусора
    val runtime = Runtime.getRuntime
    runtime.totalMemory() - runtime.freeMemory()
  }

  test("должен обработать огромный HTML без утечки памяти") {
    // Создаём HTML с заголовком и очень большим количеством повторяющихся символов
    val prefix = "<html><head><title>Test Title</title></head><body>"
    val hugeContent = "a" * (10 * 1024 * 1024) // 10 МБ данных
    val suffix = "</body></html>"
    val html = prefix + hugeContent + suffix

    // Создаём поток байтов из строки
    val stream: Stream[IO, Byte] = Stream.emits(html.getBytes(StandardCharsets.UTF_8)).covary[IO]
    val parser = HTMLStreamParser.Impl.make[IO]

    // Измеряем память до обработки
    val memoryBefore = usedMemory

    // Обрабатываем поток и получаем результат
    val result = parser.title(stream).unsafeRunSync()

    // Измеряем память после обработки
    val memoryAfter = usedMemory

    // Проверяем корректность извлечённого заголовка
    result shouldBe Right("Test Title")

    // Проверяем, что разница в памяти не превышает допустимый порог (например, 5 МБ)
    val memoryDiff = memoryAfter - memoryBefore
    info(s"Memory used before: $memoryBefore bytes, after: $memoryAfter bytes, diff: $memoryDiff bytes")
    memoryDiff should be < (5 * 1024 * 1024L)
  }
}
