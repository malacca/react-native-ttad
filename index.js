
import API from './src/Api';
module.exports = {
    ...API,
    get TTadBanner() {
        return require('./src/TTadBanner').default;
    },
    get TTadFeed() {
        return require('./src/TTadFeed').default;
    },
    get TTAdInteraction() {
        return require('./src/TTadInteraction').default;
    },
    get TTAdDraw() {
        return require('./src/TTadDraw').default;
    },
    get TTAdSplash() {
        return require('./src/TTadSplash').default;
    },
};
