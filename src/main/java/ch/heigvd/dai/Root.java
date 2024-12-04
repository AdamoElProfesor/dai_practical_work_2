package ch.heigvd.dai;

import picocli.CommandLine;

@CommandLine.Command(
        description = "A small game to experiment with TCP.",
        version = "1.0.0",
        subcommands = {
                Server.class,
                Client.class,
        },
        scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true)
public class Root {}