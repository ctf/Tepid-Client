package ca.mcgill.science.tepid.client;

public class CurrentUser {
	
	public String user, domain;
	
	private CurrentUser() {
	}
	
	public static CurrentUser getCurrentUser() {
		CurrentUser cu = new CurrentUser();
		try {
			byte[] out = new byte[1024];
			Process p = new ProcessBuilder("whoami", "/upn").start();
			int bytes = p.getInputStream().read(out);
			p.getInputStream().close();
			String[] result = new String(out, 0, bytes).trim().split("@");
			cu.user = result[0];
			if (result.length > 1) cu.domain = result[1];
		} catch (Exception e) {
			try {
				byte[] out = new byte[1024];
				Process p = new ProcessBuilder("whoami").start();
				int bytes = p.getInputStream().read(out);
				p.getInputStream().close();
				String[] result = new String(out, 0, bytes).trim().split("\\\\");
				cu.user = result[0];
				if (result.length > 1) {
					cu.domain = result[0];
					cu.user = result[1];
				}
			} catch (Exception e1) {
				e.printStackTrace();
				e1.printStackTrace();
			}
		}
		return cu;
	}
	
	public static void main(String[] args) {
		System.out.println(getCurrentUser());
	}

	@Override
	public String toString() {
		return "CurrentUser [user=" + user + ", domain=" + domain + "]";
	}
	
}
