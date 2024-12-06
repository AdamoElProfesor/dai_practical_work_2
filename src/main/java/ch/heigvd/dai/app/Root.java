package ch.heigvd.dai.app;

import ch.heigvd.dai.network.Client;
import ch.heigvd.dai.network.Server;
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