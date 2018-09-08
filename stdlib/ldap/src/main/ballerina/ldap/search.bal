public int ONELEVEL_SCOPE = 1;

public int SUBTREE_SCOPE = 2;

public type SearchControls object {

    private int searchScope,
    private int timeLimit,
    private int countLimit,
    private boolean derefLink,
    private boolean returnObj,
    private string[] attributesToReturn,

    public new (){
        searchScope = ONELEVEL_SCOPE;
        timeLimit = 0;
        countLimit = 0;
        derefLink = false;
        returnObj = false;
        attributesToReturn = [];
    }

    public function setSearchScope(int val){
        searchScope = val;
    }
};