package xyz.swwarehouse.tmpdir.util;

import javax.servlet.http.HttpServletRequest;

public class Util {
	static public String getClientInfo(final HttpServletRequest req) {
		String addr = req.getHeader("X-FORWARDED-FOR");
		if (addr == null)
			addr = req.getRemoteAddr();
		return addr + ":" + req.getRemotePort();
	}
	
	static public String getLocalInfo(final HttpServletRequest req) {
		return req.getLocalAddr() + ":" + req.getLocalPort();
	}
	
	static public String requestInfoToString(final HttpServletRequest req) {
		return "Request " + req.getMethod() + " (" + getClientInfo(req) + "->" + getLocalInfo(req) + "), " + req.getRequestURL();
	}
}
