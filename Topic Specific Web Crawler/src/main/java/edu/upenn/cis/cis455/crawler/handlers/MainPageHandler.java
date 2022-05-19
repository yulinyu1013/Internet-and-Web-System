package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Response;
import spark.Route;

public class MainPageHandler implements Route {

	@Override
	public Object handle(Request req, Response res) throws Exception {
		StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>Welcome</h1>");
        sb.append("New user: ");
        sb.append("<a href=\"/register.html\">Register</a>");
        sb.append("Returning user: ");
        sb.append("<a href=\"/login-form.html\">Login</a>");
        sb.append("</body></html>");
        res.type("text/html");
        res.body(sb.toString());
		return null;
	}

}
