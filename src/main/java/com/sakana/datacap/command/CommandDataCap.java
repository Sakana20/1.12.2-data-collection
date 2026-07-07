package com.sakana.datacap.command;

import com.sakana.datacap.capture.CaptureRecorder;
import com.sakana.datacap.capture.CaptureStatus;
import com.sakana.datacap.capture.ResetResult;
import com.sakana.datacap.capture.StopResult;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandDataCap extends CommandBase {
    private final CaptureRecorder recorder;

    public CommandDataCap(CaptureRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public String getName() {
        return "datacap";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/datacap <start|stop|status|reset>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            send(sender, getUsage(sender));
            return;
        }

        if ("start".equalsIgnoreCase(args[0])) {
            executeStart(sender);
        } else if ("stop".equalsIgnoreCase(args[0])) {
            executeStop(server, sender);
        } else if ("status".equalsIgnoreCase(args[0])) {
            executeStatus(sender);
        } else if ("reset".equalsIgnoreCase(args[0])) {
            executeReset(sender);
        } else {
            send(sender, getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server,
            ICommandSender sender,
            String[] args,
            @Nullable BlockPos targetPos
    ) {
        if (args.length == 1) {
            List<String> options = new ArrayList<String>();
            options.add("start");
            options.add("stop");
            options.add("status");
            options.add("reset");
            return getListOfStringsMatchingLastWord(args, options);
        }

        return Collections.emptyList();
    }

    private void executeStart(ICommandSender sender) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (recorder.start(player)) {
            send(sender, "Data capture started for " + player.getName() + ".");
        } else {
            send(sender, "Data capture is already running.");
        }
    }

    private void executeStop(MinecraftServer server, ICommandSender sender) throws CommandException {
        try {
            StopResult result = recorder.stop(server);
            if (!result.wasRecordingActive()) {
                send(sender, "Data capture is not running.");
                return;
            }

            send(sender, String.format(
                    "Saved %d samples to %s. Duration: %.3fs, distance2d: %.3f, distance3d: %.3f.",
                    result.getSampleCount(),
                    result.getOutputFile().getPath(),
                    result.getDurationSeconds(),
                    result.getTotalDistance2d(),
                    result.getTotalDistance3d()
            ));
        } catch (IOException exception) {
            throw new CommandException("Failed to save data capture: " + exception.getMessage());
        }
    }

    private void executeStatus(ICommandSender sender) {
        CaptureStatus status = recorder.getStatus();
        if (!status.isRecording()) {
            send(sender, String.format(
                    "Data capture is stopped. Buffered samples: %d, distance2d: %.3f, distance3d: %.3f.",
                    status.getSampleCount(),
                    status.getTotalDistance2d(),
                    status.getTotalDistance3d()
            ));
            return;
        }

        send(sender, String.format(
                "Data capture is running for %s. Samples: %d, duration: %.3fs, distance2d: %.3f, distance3d: %.3f.",
                status.getPlayerName(),
                status.getSampleCount(),
                status.getDurationSeconds(),
                status.getTotalDistance2d(),
                status.getTotalDistance3d()
        ));
    }

    private void executeReset(ICommandSender sender) {
        ResetResult result = recorder.reset();
        send(sender, "Data capture reset. Removed " + result.getRemovedSampleCount() + " buffered samples.");
    }

    private void send(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString(message));
    }
}
