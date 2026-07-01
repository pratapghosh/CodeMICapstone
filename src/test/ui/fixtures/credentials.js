function buildUser(prefix = 'pwuser') {
    const stamp = `${Date.now()}${Math.floor(Math.random() * 1000)}`;

    return {
        username: `${prefix}_${stamp}`,
        fullName: `Playwright ${stamp}`,
        password: `P@ssword_${stamp}`
    };
}

module.exports = {
    buildUser
};