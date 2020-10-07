package com.monovore.decline.effect

import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.syntax.all._

import com.monovore.decline._

import scala.language.higherKinds

abstract class CommandIOApp(
    name: String,
    header: String,
    helpFlag: Boolean = true,
    version: String = ""
) extends IOApp {

  def main: Opts[IO[ExitCode]]

  override final def run(args: List[String]): IO[ExitCode] =
    CommandIOApp
      .run[IO](
        name = name,
        header = header,
        helpFlag = helpFlag,
        version = Option(version).filter(_.nonEmpty),
        renderHelp = help => renderHelp(help)
      )(main, args)

  /**
   * This method can be overridden to replace the default renderer with a custom one.
   */
  def renderHelp(help: Help): String = Help.defaultRenderer(help)
}

object CommandIOApp {

  def run[F[_]](
      name: String,
      header: String,
      helpFlag: Boolean = true,
      version: Option[String] = None,
      renderHelp: Help => String = Help.defaultRenderer
  )(opts: Opts[F[ExitCode]], args: List[String])(implicit F: Sync[F]): F[ExitCode] =
    run(
      command = Command(name, header, helpFlag)(version.map(addVersionFlag(opts)).getOrElse(opts)),
      args = args,
      renderHelp = renderHelp
    )

  def run[F[_]](command: Command[F[ExitCode]], args: List[String], renderHelp: Help => String)(
      implicit F: Sync[F]
  ): F[ExitCode] =
    for {
      parseResult <- F.delay(command.parse(PlatformApp.ambientArgs getOrElse args, sys.env))
      exitCode <- parseResult.fold(printHelp[F](_, renderHelp), identity)
    } yield exitCode

  private[CommandIOApp] def printHelp[F[_]: Sync](
      help: Help,
      renderHelp: Help => String
  ): F[ExitCode] =
    Sync[F].delay(System.err.println(renderHelp(help))).as {
      if (help.errors.nonEmpty) ExitCode.Error
      else ExitCode.Success
    }

  private[CommandIOApp] def addVersionFlag[F[_]: Sync](
      opts: Opts[F[ExitCode]]
  )(version: String): Opts[F[ExitCode]] = {
    val flag = Opts.flag(
      long = "version",
      short = "v",
      help = "Print the version number and exit.",
      visibility = Visibility.Partial
    )

    flag.as(Sync[F].delay(System.out.println(version)).as(ExitCode.Success)) orElse opts
  }

}
