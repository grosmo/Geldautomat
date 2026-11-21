package geldautomat.service;

import java.util.Arrays;

public class Message {

    private String command;
    private String[] param;

    public Message(String command, String ... param)
    {
        this.command = command;
        this.param = param;
    }

    public String encode()
    {
        if(param.length != 0)
        {
            return command + " " + String.join(" ", param) + "\r\n";
        }
        else
        {
            return command + "\r\n";
        }
    }

    public static Message decode(String message)
    {
        if(message.isBlank())
        {
            throw new IllegalArgumentException("Message cannot be empty!");
        }

        String[] parts = message.split("\\s+"); //trennt an whitespaces, auch mehrere aufeinader folgende
        String newCommand = parts[0];
        String[] newParam = Arrays.copyOfRange(parts, 1, parts.length);

        return new Message(newCommand, newParam);
    }

    public String getCommand()
    {
        return command;
    }

    public String[] getParam()
    {
        return param;
    }

    
}