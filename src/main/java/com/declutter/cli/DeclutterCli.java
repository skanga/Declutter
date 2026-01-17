package com.declutter.cli;

import picocli.CommandLine.Command;

@Command(
    name = "declutter",
    description = "A tool that declutters any URL into clean reading formats",
    subcommands = {ExecCommand.class, ReplCommand.class, ConvertCommand.class},
    mixinStandardHelpOptions = true)
public class DeclutterCli {}
