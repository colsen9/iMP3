package coms309;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return "Hello and welcome to COMS 309";
    }
    
    @GetMapping("/{name}")
    public String welcome(@PathVariable String name) {
        if (isHex(name)) {
            return "<style>body { background-color:"+name+" } </style>Hello and welcome to COMS 309!";
        } else {
            return "Hello and welcome to COMS 309: " + name;
        }
    }

    @GetMapping("/{var1}/{var2}")
    public String welcome(@PathVariable String var1, @PathVariable String var2) {
        if (isHex(var1)) {
            return "<style>body { background-color:"+var1+" } </style>Hello and welcome to COMS 309, "+var2+"!";
        } else if (isHex(var2)) {
            return "<style>body { background-color:"+var2+" } </style>Hello and welcome to COMS 309, "+var1+"!";
        } else {
            return "Hello and welcome to COMS 309, "+var1+" and "+var2+"!";
        }
    }

    // return true if the string is a six digit hexadecimal number
    private boolean isHex(String maybeHex) {

        // first off, check if the string is 6 characters
        if (maybeHex.length() == 6) {

            // now, check if each string is 0-9 or A-F (or a-f)
            for (char c : maybeHex.toCharArray()) {
                if (!Character.toString(c).matches("[0-9A-Fa-f]")) {
                    // if not hex, return false
                    return false;
                }
            }

            // if we survive the for loop, it was a six digit hex number, return true
            return true;
        }

        // if it wasn't six characters, then we return false
        return false;
    }
}
