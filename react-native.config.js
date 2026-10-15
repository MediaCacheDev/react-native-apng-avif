module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import dev.mediacache.apngavif.ApngAvifPackage;',
        packageInstance: 'new ApngAvifPackage()',
      },
      ios: {},
    },
  },
};
