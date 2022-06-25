package be.jorisg.ultrastarorganizer.command;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import be.jorisg.ultrastarorganizer.commands.automatch.AutomatchCommand;
import be.jorisg.ultrastarorganizer.commands.coverart.CoverArtCommand;
import be.jorisg.ultrastarorganizer.commands.reformat.ReformatCommand;
import be.jorisg.ultrastarorganizer.commands.tracklist.TracklistCommand;
import org.apache.commons.io.IOUtils;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "",
        description = {
                "UltraStar Organizer CLI tool, hit @|magenta <TAB>|@ to see available commands.",
                ""
        },
        footer = {
                "",
                "Press Ctrl-D to exit."
        },
        subcommands = {
                CommandLine.HelpCommand.class,
                PicocliCommands.ClearScreen.class,
                TracklistCommand.class,
                ReformatCommand.class,
                AutomatchCommand.class,
                CoverArtCommand.class
        })
public class CliCommands implements Callable<Integer> {

    @CommandLine.Option(names = {"--workdir"}, description = "The working directory")
    private File workDir;

    private PrintWriter out;
    private LineReader in;

    @Override
    public Integer call() throws Exception {
        if (workDir == null) {
            workDir = new File(System.getProperty("user.dir"));
        }

        // set global workdir constant
        UltrastarOrganizer.workDir = workDir;

        // setup terminal
        SystemRegistry systemRegistry = setup();
        UltrastarOrganizer.out = out;
        UltrastarOrganizer.in = in;

        // print greeting
        try (
                InputStream is = UltrastarOrganizer.class.getClassLoader().getResourceAsStream("greeting.txt");
        ) {
            String greeting = IOUtils.toString(Objects.requireNonNull(is), StandardCharsets.UTF_8);
            out.print(greeting);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // start the command interface
        out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow Working in:|@ @|white " + workDir.getAbsolutePath() + "|@"));
        out.println(CommandLine.Help.Ansi.AUTO.string("@|yellow You can change this by supplying the --workdir option on startup.|@"));
        out.println();
        out.println("Welcome! Enter your command below. Try 'help' to get help.");

        String line;
        while (true) {
            try {
                systemRegistry.cleanUp();
                line = in.readLine("> ");
                systemRegistry.execute(line);
            } catch (EndOfFileException | UserInterruptException ignored) {
                break;
            } catch (Exception e) {
                systemRegistry.trace(e);
            }
        }

        return 0;
    }

    private SystemRegistry setup() throws IOException {
        Parser parser = new DefaultParser();
        Terminal terminal = TerminalBuilder.builder().dumb(true).color(true).build();
        out = terminal.writer();

        // initialize picocli
        PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();
        factory.setTerminal(terminal);

        CommandLine cmd = new CommandLine(this, factory);
        PicocliCommands picocliCommands = new PicocliCommands(cmd);

        // initialize picocli integration with jline
        SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal,
                () -> Paths.get(System.getProperty("user.dir")), null);
        systemRegistry.setCommandRegistries(picocliCommands);
        systemRegistry.register("help", picocliCommands);

        // create line reader
        in = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(parser)
                .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                .build();

        // enable tailtip widgets
        TailTipWidgets widgets = new TailTipWidgets(in, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
        widgets.enable();

        // add key binding for toggling tailtips
        KeyMap<Binding> keyMap = in.getKeyMaps().get("main");
        keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

        return systemRegistry;
    }

}