package dk.au.daimi.tandrup.MPC.net;

import java.io.Serializable;

public class Activity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Activity parent;
	private String identifyer;
	
	public Activity(String identifyer) {
		this.identifyer = identifyer;
	}

	public Activity(Activity parent, String identifyer) {
		this.parent = parent;
		this.identifyer = identifyer;
	}

	public Activity subActivity(String identifyer) {
		return new Activity(this, identifyer);
	}

	public boolean isSubActivityOf(Activity other) {
		if (this.equals(other))
			return true;
		
		return this.parent.isSubActivityOf(other);
	}
	
	/**
	 * Is this activity is a parent of the other activity
	 */
	public boolean isParentActivityOf(Activity other) {
		if (this.equals(other))
			return true;
		
		return this.isParentActivityOf(other.parent);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj instanceof Activity) {
			Activity other = (Activity)obj;
			
			if (this.identifyer.equals(other.identifyer)) {
				if (this.parent == null && other.parent == null) {
					return true;
				} else if (this.parent != null && other.parent != null) {
					return this.parent.equals(other.parent);
				}
			}			
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((identifyer == null) ? 0 : identifyer.hashCode());
		result = PRIME * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public String toString() {
		String retVal;
		
		if (parent != null)
			retVal = parent.toString() + " -> " + identifyer;
		else
			retVal = identifyer;
		
		return retVal;
	}
	
	private int getLevel() {
		if (parent == null)
			return 1;
		else
			return 1 + parent.getLevel();
	}
	
	public Activity stripDetails(int level) {
		if (getLevel() <= level)
			return this;
		else
			return parent.stripDetails(level);
	}
	
	public Activity stripParents(int level) {
		if (level <= 1)
			return new Activity(identifyer);
		else
			return new Activity(parent.stripParents(level-1), identifyer);
	}
}
