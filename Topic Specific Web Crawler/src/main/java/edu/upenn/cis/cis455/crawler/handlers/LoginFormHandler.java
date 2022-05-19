package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Response;
import spark.Route;

public class LoginFormHandler implements Route {

	@Override
	public Object handle(Request req, Response res) throws Exception {
		StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Login</h1>");
        sb.append("<form action=\"/login\" method=\"POST\">\n");
        sb.append("    <label for=\"username\">Username:</label><br>\n");
        sb.append("    <input type=\"text\" name=\"username\" ><br>\n");
        sb.append("    <label for=\"password\">Password:</label><br>\n");
        sb.append("    <input type=\"password\" name=\"password\"><br>\n");
        sb.append("    <input type=\"submit\" value=\"Log in\">\n");
        sb.append("</form>" );
        sb.append("New user? ");
        sb.append("<a href=\"/register.html\">Register</a>");
        sb.append("</body></html>");
        res.type("text/html");
        res.body(sb.toString());
		return null;
	}

}
