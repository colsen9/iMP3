package coms309;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

import java.time.Instant;

@RestController
class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return "Hello and welcome to COMS 309";
    }
    
    @GetMapping("/{name}")
    public String welcome(@PathVariable String name) {
        return "Hello and welcome to COMS 309: " + name;
    }

    /**
     *  For getting the time.
     *  USE: localhost:8080/time/now
     * @return time value based on the user's system
     */
    @GetMapping("/time/now")
    public String time(){
        return "The time on your system is: " + Instant.now().toString();
    }

    /**
     * For reversing a string, and providing the length
     * of the string that the user provides.
     * USE: localhost:8080/reverse?text=hello (for reversing "hello")
     * @return reversed text, and length of said string
     */
    @GetMapping("/reverse")
    public Map<String, Object> reverse(@RequestParam String text){
        String reversed = new StringBuilder(text).reverse().toString();
        int length = text.length();
        return Map.of("text", text, "reversed", reversed, "length", length);
    }

    /**
     * Given a word, will return the word and confirm if it is
     * a palindrome or not.
     * USE: localhost:8080/palindrome?text=hannah
     * @return JSON with text, and if the given word is a
     * palindrome or not
     */
    @GetMapping("/palindrome")
    public Map<String, Object> palindrome(@RequestParam String text){
        return Map.of("text", text, "palindrome", isPalindrome(text));
    }

    /**
     * Helper method for determining if a given String is
     * a palindrome
     * @param String s (word to determine if palindrome or not)
     * @return boolean true if palindrome, false otherwise
     */
    private static boolean isPalindrome(String s){
        int length = s.length();
        for(int i = 0,j = length - 1; i <= j; i++, j--){
            if(s.charAt(i) != s.charAt(j)){
                return false;
            }
        }
        return true;
    }

}
