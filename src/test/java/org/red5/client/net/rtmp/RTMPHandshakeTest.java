package org.red5.client.net.rtmp;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.red5.io.utils.IOUtils;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTMPHandshakeTest {

    private Logger log = LoggerFactory.getLogger(RTMPHandshakeTest.class);

    private IoBuffer clientC0C1;

    private IoBuffer clientC2;

    private IoBuffer serverS0S1S2part1;

    private IoBuffer serverS0S1S2part2;

    private String youtubeC0C1 = "000000000a002d0235c46e929009dde8c80b089c3026c0938c1fa6415bff483969db1468d34f1c884457b87da48b1f17d2e39645da5a9c16af84ada92e69c5c4074e17098ff957ac1a1cf790ef1791055d45fb21c0c07c070cbc32cf0335a45c61a2034fd6c26207cf2cb43ae3dded6af8df61562548e7162f543174d6921349c7be44e484384aa181215342a64ce1c3d43a0114200a80f0e7b59ada97b9b85e6db722338d49b3f0d14c3a4a94d582aa4885f73c1d0fceecc6a8753dab512608eb26ddfe75f5944c630816d3bb7eb8364ef7e10b5c656263a4e7b9a76cafc5eebe1688c7d64a2a40621189025e45695003c2cf2571875b491cc559fea4ddbc90c4776df8deb5b5f2f9f4dba77350a94f2a907210c2127f8d0de02b8f16789b78c42af556ce9eefaddb8c7df925f349e2aa96aea00dce63d01ae8a63af555d8dae49855267c583a4fbb8b664e527f77f10c35ec97dfff65ac2b72a15a6b046a1431dc8efad25ef518d519b2a60de84e2dfe25cd501d1560beeadc87dc141104e51b7872434de589ea6481fd2e1d3495f7d23f270e7f9761054745baa18024c346f5a1b92e7eb50a5f29eb600a7dc0faa1ffc466391074defcf58a041db4ec517479196c9e8c5d81b3eb2a1397da5294579f44ba9fb091100aea646250a9d7d00543911997b1ae429ff19b0a1218fc7394f00aaa8792b78572e38581c2cc9d0a0c51a7a7b7b9dc8354004bf4c42857297d0270fa807af3e24b9621d232352a3c3514434f1de607af2385bfe0b3c54d446650e10dfb9031ad9b9d9d433e900012723382dd7f8ed3a226b96e10667ce670641010f07737f0cf79290d5c0acfc05c267bb843015bfe6e6f7f71f4f9a7eb8bdee2930a4d378988b84d6ef7717568ff54f05053a80a02f0ce32e432d7ab9b64bb36b20698e79643aa18b28585b4b1e88708c28c9acd22122eb6e5287624dfa835a5e03639f48ae71d5cf03c2630f0e9e89a10bab5691d31ffefe36e7a60dbf4886725d78eddc4f0e531fc4587824b1d3562b804f9b8254f0145f8e9d7e2545299db07c90674f889757208377bb7c9e88f8024b0c62935bdda2654fc40dfe151650311ade463e4da983e5c8ee3cf29689ef9b77a183ba90e6a84bda69cc4ec272e05dc7bb4a55807fb78171fdc3e8e47e33566e73e053272ed531bad8bae867784abb60004088d98414807a2c86df7588a7fdbf1c9b110ca490c121014f009b5d3e4d1d727b2c74a02998adc3e3a8e6eb28935efc7fda51dcc2db06d62918c4a994102d202995379bd98860fe474be43cbe54b9d9db9503eb9b72a1b31cb22e82f7810e00009c7a2543b6ba0b860217fcc8c88a2584b9cb9e0afa909cd8df1a9fae599e7b8229abf29b8472ef9373817e25b5000842ebf72319d24c8e0fa10fec6048123f3580d653109ef84b0a5f6a3a992fb39346e697dff104f3426e83e746ff8edf123f15feec66f2c20524c1d6743535e32372de313441795acdf6261e12408df31c58a5626ffec0731393f64154929e10112227bc2a1c927d58127d980461c49925a93112b8bc71a57b48775aa337e2947bd15452260bf19424728a4fd5c44c87457ce3386fa0cf47f5aa2b7d1d82352b4de6428bde3958b987f0e508d9f31e0458fd458b3500a8d9ec1fee7ad6fccb3da94823ef55f6ce8f84bb1bc8665bacc6c5390db8e6fd3cd2c54838509fc913d77c8728109a26b32c422a2ae1e0a59e234e4c2ec66cdbf21a9ee060b3a166f5dc15c50ca91dd395f0ee5a2313413ac7e52647df9a84ae91b8222d7f93fc5c5787e86c81f0c5485c44f8829cb56dff488e510a96165c0b02b227834b4ed87d8f4db079d54558a7f942757e07c70ba99821b1a827e230e38610dced3271fb94fcf510d24f626805c6a41942797bdfba427d31a17f9bf02e74143ff5c484c32b7c60feec68af406d5e6afda905bfcba439fe3d7cc704f79e8ac564fed59006f0013b7208220c2e6a85c4eef626ffebd483ce9a1d8713f68abcfedb6940b007bfead624b682bcef8168dfbd6171e067babcdb96ec8bb2f6e632a9c0fd86128a4e92b40741fb428c16474bfedde6b996f047fe281fad73d9d86a4484a31715040dd233bfe60d691595a262e6b19f8b4961149d1452300bde251b1914f8be8b7d6";

    private String youtubeS1 = "000000000400000126b33989118ea1422bc3c3317eb7a6774d6c0f5dafc8ea157885c76344adfe087437e66f8ce198081e6c6cc3a340c17a7ad8483a62534ed21f2894f4f6189787be97d83cffd2e98ebf787313885757746896d4cd1aefee23b32ccf7d36036ecf913dfa49930d69a725992ba9a5790254fcd7affbe902689ce7c94b5033f313090d30a78f79860e7bcf5882e52cd70d486d4f19cae1b56bdc8693d80c5b6e413fa8965b76b5acfaafe926c6ec2b95fbb5f4e680a883251a5bfe40fe47900be1929c4617803522fa2dcc349e9ea4d35f8d37222a0831ecde078854e5a8237ff2ee67b7a6326985e241de02fd1829c1dd3572af76d1df23ef4519a8463d4e69759ae1a350602c89473f3ace601da88174f92c3e267c7f84b82acee80df6d660dccff6fd4aa9c02ee28bc1df77566d93a1651fcf6c89bc5c9d1dc0b0e2cdaafe0a028b6f5a825a3751d596ef413abf198f9959869b4d906a70eb660e977e62e2b6519b28ca9431780875b6f5354d0adc0ac21eb9e2197433af49ac7cbb6b20eb5c21a2eb3c72159b4cdea534f324ecee0aec977aea152b043eaa1e5c03a147ac1cee8fa17e0536351d2e9e20c1c6f1bfa4d7aca02ef01a01aa5f68e82667646d074817a5b884a0021e0d54d8ea88fb6bcc618e1bff170fed08844cdc21dc077383d137b31a0a502313404423f4d1a39d29e0706e0e1d36c3a3eb10bddbf7cc1ec19d73bf7df5c8e310d06966465fb1a1c61e7fa476ec5615a75597f8c397443401bf82849af05ac48dd11d959a9708512827293f8d404913e779e31197c2f1e927dbc5f6585ba06297d975f4c2e0d5102430340de92d86a707fb096ba2d9bf2b02f4d5023f010be0b718b88b462af971ca7d0c59c0f5098e08ae6967bb169a5365a6be5a2974d43a287fcdff34bbb54471a8914d63cbb763d3ccf7a63974dc30affd3d10e1fc7ded4b138ab0ed8c539c2182d7f950c793be8150229e1a96803459bdaaad254179f361a407e10cb4a2fd2fcbd628ae363cbba7b27da583e95cc5e0b4d493f8b17258351adde40d2e8fce8cd0454ad49e3a128cc689f45ba714903f49733b7ead91a965a3ba65b4914139de79d29afb60fe97426c315d7fde490783e2922e3c20af94d7f2bd1e3403436d564462d8c3295e1e9db3972c574a0669312cad2576d37be5bfcf5ca9ae618c7f45fb78771e773f3c68afbd3f013356c6389f38c75f28d244c0431bc93ecd1ced40ceb880ddea7d93ac5efbad9ab2df10313343bee0523108f52c97180b64c73257868a6968859bac5326aa54d2f85ecac814102f3f3fac8b953752163a3097b8e7912c1f56359d3149d6722d59fa2dfe54a8fd0dc6752750824a1f43cf03e30343b1ef0e63d35191b05eec3655e9a60dfed78e1827d7b15f458865eacced082dbfb3355b3390d4a9ff8f25f8cb61e4844a23f102bc17d4bf5308f0264524f0cfd0a5eaa42b5fbafd9736efe8ea5907d22f293a4ddadfb9dac8d522c7a09adb6f38d0c294f54d043d053b428e5c9d73d85b966a61639a60a6a164d50fa4bc510caab2ddd75266cd41e16fdd61f96dc54d243395f1a2bdc00c532ea179fc78d29f91eaf678235b498244d2323e83227cfd3977d4b28c689a769a268240d5c9b337858bcce23d66d0aa85da16a0dc017871bcc90494489db1682f88296278b32e829d5673e0d54310eed45be4fd3e9d732843b55c6b902c57a65ff5735331be1e62005cbaa4acf27ca208c6daff6a6b6dc7e25d88ef43f469435b78ffeb243c7cfc1c0483b0e47e11586c533c5c74f662cf7a1e0d037aa7d722fb348d9fff88b2244f44b7bab8ee3cf5425fdf6bc6f6b6395cdd4dd0fda76c89aed47aca19298118ab55b1c2bcf54dfb91c4f42a66aa808d68780f49b20adef3de077b38a9701e2d2fa0f8d5c42413da26b0c0480c6824dd019924c4a1ab3edeb38d43fd8cd17fc1898c777826a20710f1b973f72532156abb8e72285516a372f1821e0f016b08a0844bd8f10e9163913e9fb570aea5499123c0b7e3549ca99b5aa2f784a6cf2e49d7bc192b40e0a8af31ed9296280c12a9131e70eb7e8aa6530879e06c87ad52e0ef1206d643be317c72bc1a4cb1cd28f5c2a57ccde6fd8591ee4b18c18af86e04b9cd3df18e0ae2af4185000000000a002d0235c46e929009dde8c80b089c3026c0938c1fa6415bff483969db1468d34f1c884457b87da48b1f17d2e39645da5a9c16af84ada92e69c5c4074e17098ff957ac1a1cf790ef1791055d45fb21c0c07c070cbc32cf0335a45c61a2034fd6c26207cf2cb43ae3dded6af8df61562548e7162f543174d6921349c7be44e484384aa181215342a64ce1c3d43a0114200a80f0e7b59ada97b9b85e6db722338d49b3f0d14c3a4a94d582aa4885f73c1d0fceecc6a8753dab512608eb26ddfe75f5944c630816d3bb7eb8364ef7e10b5c656263a4e7b9a76cafc5eebe1688c7d64a2a40621189025e45695003c2cf2571875b491cc559fea4ddbc90c4776df8deb5b5f2f9f4dba77350a94f2a907210c2127f8d0de02b8f16789b78c42af556ce9eefaddb8c7df925f349e2aa96aea00dce63d01ae8a63af555d8dae49855267c583a4fbb8b664e527f77f10c35ec97dfff65ac2b72a15a6b046a1431dc8efad25ef518d519b2a60de84e2dfe25cd501d1560beeadc87dc141104e51b7872434de589ea6481fd2e1d3495f7d23f270e7f9761054745baa18024c346f5a1b92e7eb50a5f29eb600a7dc0faa1ffc466391074defcf58a041db4ec517479196c9e8c5d81b3eb2a1397da5294579f44ba9fb091100aea646250a9d7d00543911997b1ae429ff19b0a1218fc7394f00aaa8792b78572e38581c2cc9d0a0c51a7a7b7b9dc8354004bf4c42857297d0270fa807af3e24b9621d232352a3c3514434f1de607af2385bfe0b3c54d446650e10dfb9031ad9b9d9d433e900012723382dd7f8ed3a226b96e10667ce670641010f07737f0cf79290d5c0acfc05c267bb843015bfe6e6f7f71f4f9a7eb8bdee2930a4d378988b84d6ef7717568ff54f05053a80a02f0ce32e432d7ab9b64bb36b20698e79643aa18b28585b4b1e88708c28c9acd22122eb6e5287624dfa835a5e03639f48ae71d5cf03c2630f0e9e89a10bab5691d31ffefe36e7a60dbf4886725d78eddc4f0e531fc4587824b1d3562b804f9b8254f0145f8e9d7e2545299db07c90674f889757208377bb7c9e88f8024b0c62935bdda2654fc40dfe151650311ade463e4da983e5c8ee3cf29689ef9b77a183ba90e6a84bda69cc4ec272e05dc7bb4a55807fb78171fdc3e8e47e33566e73e053272ed531bad8bae867784abb60004088d98414807a2c86df7588a7fdbf1c9b110ca490c121014f009b5d3e4d1d727b2c74a02998adc3e3a8e6eb28935efc7fda51dcc2db06d62918c4a994102d202995379bd98860fe474be43cbe54b9d9db9503eb9b72a1b31cb22e82f7810e00009c7a2543b6ba0b860217fcc8c88a2584b9cb9e0afa909cd8df1a9fae599e7b8229abf29b8472ef9373817e25b5000842ebf72319d24c8e0fa10fec6048123f3580d653109ef84b0a5f6a3a992fb39346e697dff104f3426e83e746ff8edf123f15feec66f2c20524c1d6743535e32372de313441795acdf6261e12408df31c58a5626ffec0731393f64154929e10112227bc2a1c927d58127d980461c49925a93112b8bc71a57b48775aa337e2947bd15452260bf19424728a4fd5c44c87457ce3386fa0cf47f5aa2b7d1d82352b4de6428bde3958b987f0e508d9f31e0458fd458b3500a8d9ec1fee7ad6fccb3da94823ef55f6ce8f84bb1bc8665bacc6c5390db8e6fd3cd2c54838509fc913d77c8728109a26b32c422a2ae1e0a59e234e4c2ec66cdbf21a9ee060b3a166f5dc15c50ca91dd395f0ee5a2313413ac7e52647df9a84ae91b8222d7f93fc5c5787e86c81f0c5485c44f8829cb56dff488e510a96165c0b02b227834b4ed87d8f4db079d54558a7f942757e07c70ba99821b1a827e230e38610dced3271fb94fcf510d24f626805c6a41942797bdfba427d31a17f9bf02e74143ff5c484c32b7c60feec68af406d5e6afda905bfcba439fe3d7cc704f79e8ac564fed59006f0013b7208220c2e6a85c4eef626ffebd483ce9a1d8713f68abcfedb6940b007bfead624b682bcef8168dfbd6171e067babcdb96ec8bb2f6e632a9c0fd86128a4e92b40741fb428c16474bfedde6b996f047fe281fad73d9d86a4484a31715040dd233bfe60d691595a262e6b19f8b4961149d1452300bde251b1914f8be8b7d6";

    // Chrome version 47.0.2526.111 (64-bit) FP version: 20.0.0.267
    private String chromeFlashPlayerC0C1 = "0000068e8000070282a38d183ee941036bd4fdf6b99369ee3f9b2af7f271a28c135329e29bde85bf67fd607fc566160fb62aeb4f190b935c711a6acf1111fa8bea7c677f78b84e2b6e373a931414727350535544221361fc9180aa71bacbcdf94ef6019038c03e06da0a0069d9e1cd7fcd275ae5f389683a835471fd0fb47cfde79af2bafefe3865a1e5cdd8b8a118a742f48ed1326ff84249de2f9f319f9efd672fde65a0ada9a3e57305d30efdb5cecb6924d5a7c8b36cf9a60c53be6c53077d2cda7ac3eeed8c29b40e0b3e06f0bfe7f167aa1c37f7f774b4c7dbb8721b76c6a22fe9c12640b824a8c88caaa5b2b0df3555504fb180b450a4c2b67570873de12c5790e501c05584fc3232103ce8e33ecc294b32356ec0d803903deeac831e5b7bae67626c8f403b664f9ab4abd936c31be19fcdac191faaea6dc177a9513fdddfedf43678663e149a6ca8abf57cc39049436bd14e5bf16f385733c053186b69a096acb0e2f8db7ed5e67cbfc9ab91165fb3b7911c7f6f97c8f667dbe6588fd93fbd77b418fd57557b9adba503c17f23237d84fde0d1458d4295de23bec4da78fd2722f3746213aa6f9a4a8159c0545129aeecd12803b419e31d49b9bb2183a46e90e5231c65c2b8b53ada899f3a271cdf37690c166b1719374ebaddd727e95d94df74e02bb375dc194a92264965c4a7a94b5e26b4fb1e7dfc488fd1156c83906cda5ce20dabefbaf8085c92a45cd373e856ee1baed027068c4f3d3694c2c2f31d85b445b416bc97222fa7fd9f5ee43d583d360c5223de8894beda51c9ae457fa5e6af9fb12c97927b3123da3bec972bfccedc5b811931fb7fd57d4392ab5b7776833f0b0e9e80138b161e62f65daec5abbec03e48572f8185b787c19b91eacdc0c12dff4d0fb0a0a0cfe78581ffcf32159b2e949ad40f6eee574233f5c1faa955e3f6f0ac232c0b4c6443b8d5e48f0531cdcaae2ff52d4d3def1bf174f12bd093057dfe9170ca6e717d2f3043c998d3a20ed5ddb80815510647aa06c672ba45bdd9ee282b337ceef0c2eb9867ee1d65bb251d0695afc1778ed258953af0b1752f78babc2a0643f7df068b5dea9344d667facf4ab9e9073cf235260bbbe23618118e7b1fedabd106c7e9c786b780ca22254694f3614374a840dcdfa32bdf44d1ec1f0128ebb8d988d350bc75e286fbf9142d19c4e2f450dcadfacaa0c380c6e3c34cd61be34bf83cf793ab0f84fe2ab6c9d6a4676decab9c6aac0e3ae7da6808b63b8cd048b44c9f7d432675632d7a553a95a47126e987e2d53d948672d12e8a2953d7cae8611690907b1f32e98f754c29ac7c2b3fecb8e1f70bc71a38a37a82e18909c52476e34d3eb3e7d5f03035b76b3af6e50c8f7ba58ec9b29d0b34d538502ecc57f1093044654df78768e6edef7627f700b4724ac2e4d0ae3d1ef87364e50d88bda26808b4e5ae4ec9732d5deb29f0d367173e1be0a3a2c94cec282a8c636fd1fa179f23da3ad2555611c182ef63e8bc0568c24dda5f1fe26e6ab955626cd2ee4e56ae0a7e1b78cbad84116454a96f116ce6a83241a3a4f4b38168efea45c8c80c67829260aab28805c8e57b2ab37fafa104be04568c36f4d56493aed0c96eb5c5635b67874c0bb406ed780d6eb20217388fbf38960064162fc429c9aaa43a7f87a4beba8e6e1a6d9d5c4e1a40223d7f917e9762594fb56e279c693b7aa6dc2bdebec8bcf47928850d3acaf551d6e2b39b522f77ddcc69f87622539fe37fd922f15a4530e8d36bfbf5d59245e411e223b8a1e2474bce6976c75c020a948fe857375c474a3e470fde2f90756f62cd25a094c320544be6448a2b50743b1b4f4a9e03160927a6a327427ff9da1cd7303fc041cadfed03a0cfe748b64a64041db58e3329ec76765b0282afb5fc93d19c6f72d4106bb32fd94747ba567381728b89456c7498eb8281c9f1c50df6b41574e4a375a1562233cc44eb6d5d36f8380a5a6fa035403c07bd5a5a1260ebb50225162310fd2c813931870f29beb9b8c161ec8ace4478979a34a53f1fa4be9674dcfea0b5747022d86e936ec045a4c4bd80c75fd3e60702d54222ac8d529ee9fdf8b0b0ca4fd2f987430a9741db0f2c0073656216fe0d3385801ac1e9721dc74cc1858e5c95b36c9f57a91578beef80";

    // Firefox version: 43.0.4 FP version: 11.2.202.559
    private String firefoxFlashPlayerC0C1 = "000006888000070276516e1353d7e9d3bdf8aed918f03ecb0b83a50b9ea618823fb76e86c29b8976a2188673150d438cfc9340d75011594687380a44429bd536ce2f9f10d718067d387f13e82ace4b2730849c4f3c94be7e42a55517aecf836a131b65cedba2d89b7cb83983cbf840f029aa8988d012daf2edd78146cab63d43a094fa65dea5a5aef719146b317c22af43cafa41a58963f3dad4edb3361ebc7bee2efc2f3367ffcd48f02d79e7dbb5e3025881c787f7cd2cf74ccbdc52ff44c4feea9a3786340020de94dd25f341f37e76726a3578426beaffc7dbe747b99995608e971461dcd6b2a695a90501c9cf8582317453075045441c0205ec46c4386ea946c23de5457147b8733809c9f25fb716c90b2696078c7450f5c576a3be763f4b2a2815f998f1d9a68a5b1a63ad544526fdd90d04df0ffd116fbf73dc054f67fcad5e95b42687d72be33c132c67ce79015c5ba134504d77ba2ced3c24d271468dda43a51ebdeb386c5d492b7c6b2e5e0fe135c77db709e988a8c31999442a387146c7718d71deb6bc0ddd61edbf9643511fbc0cd83fe94df3b9090e858642530f8d61e996e6208a6f7e1628547a9d3319c51a39b181f144667365a07c009b7710520875d5297372d1bf3a32fe5281cd4efd42a9b17c07cfa3b68056b8064412cd7d5fb733020233ab20d3ca8a9b40f6a3647aeabb8e8bd76044cb190d78fb52098dde1bcd9b6ff12c50cfa8a802505005832d95196fcba0a0c1ae478040766d9bb97a34fa1aab3cd8dcaf9fed674801041744112c9de3f0e4fca3eb82a38614d259748fe4688ecbd57896dbbd677714e194650224dd6b14d66a5458f292ae5e1e370e682715b6e7b6ffb4d9200d8ff58513acc585e37d15ad3302da406caafa8f791c6c29facb933cc0c3646f8675208c47510319cc0ba77071611ac9104ca0e2249db77e0232aa57dab2e6f99037487b0c1a26447aa682654ad255bf14eb16408e4882651b610e58edc9168459d91d1b5c900ff15515ad5c5249a22bdf2a1abe15dddbe53beab93762662edeee18d607e6848ad63d348b09abf5420081a2b912ac96adb786ac3304dbeddcdf56cc0751dd3e45ceaea115c33418160885d816d6a1e6f5fab248c6be60ad9cfb96e2ba4066fa5a880742bd922781e0f72e48ba53cbcb0e3aa2b322f38734dd43671861da95c57b3b41f10feccbfd4d57239577b90dfe6a464c2928d679a08b86f4adb87a088d8574763452cf5b6608ea37502d675d013895826b5697fc914de21d7d23c8b1ec752915b251a2bb1f52c3a9d161ebb6450c24a4afd113c9a690676602fe3bcab498d134735a096149b70cb78bfa153691f1edc65c23938ff6b77989e8392470897082f4168e98668455a31effd10d53fed0bd7b2d6c0fd24cf70e7783f635f4086c6a463aba6e63b16563334efafcb1569d5f77e337d6aee6900f2e9edd43fa98cd666342d3d9e5cc949eb37ed12d20909a5d1c5945bebd39ff7772a38d9b9bccf544eaef67a8a6f2fbd622c42b098e37876d8accc710806b4132c61632405a560adfc82cff1bd9cd0d4baf2bd18a054b503acb99ff396207d3c625ce912eeebf88cca68a19ff10b1c5c2341dc11df1155f018ce000a91c9a911c7e2a919c0a19d023de5107c40a64bb8529f4f457b471d439a6a73dbc1c9d8190ceca5aa3e1634445f82efdeceae3c305b848c921a71de58a7b887f78736e0ad51cb1b39d4c137bc6d037c4fb663facbf53376052bfb52441fd9e2e90f663370864ac0e7a9635d7d62ef0a09cf1e768ee9102fc31180ff193db4a29928ce5249010a36d6dee26d783d0dc8bccd472a8a7ef88d6626f7816d5ddb65e7ba266939bfe47343f5007792bf20308e0040a4e2696ae8f064326dba9ce18aed06b082735d5429275d34830b1df46366c2cf363ab6e05e94bd3a15c70251f3af3d2f4d85dd99bd3f26708c365b296ba85e3ba0cf9688b597636058012013b3686e092814f237d3b7f74a7a0024ffd66580825810df1106fe013f503c4677c35e377dc0ee97de324a11bd3c1bd829119bf86ca5b47b17ad268735ac59db02584ccbce702eebb464f10c2c6f921de68a43c83491b94d4201b7bbca037c6b70ecb3ed7737feb7484a9a9fa0d02dd6594238486c2371112e1f9";

    // FFMpeg version: 2.4.8 with libRTMP
    private String ffmpegC0C1 = "366778720000000067458b6bc6237b3269983c647348336651dcb074ff5c49194a94e82aec585562291f8e23cd7ce846ba581b3dabd77e50f241b12efb1eb741e3a9e27946e145757c005f51c262d05b54082012f827b14d1b231602e8e9161fe7cd90118d43ef66760f0e145a2552332ef99c106372ed0d33c2dc7f9fd7ef1bc9c4a7419a07686b66fb6a4e325de4250d509b51b7d71b4331ba2d3f58e4837ca33071255ad9bb6225616c435d898c6205b13a3317a31d7258a84324e95a1d2d5e846367d4a8a275abbded08b28c8379cdd05343c6e0030b9b769a18b49ee4545424f3711186a82c0ec43608821d900274f8953a4186130821f57f1e3dbd3d7cdc8d7b7387f0ea6c701a2222e9dd16453ec80630a1d44f6141c29a41e1f87755fcad0b44672307053e820438015f46777ec62477972a485ceab96324dc4a885e6bd3ea519677512d8fd70b5838a43e155c5855382a4ea670ec42236ab07c482a3bd44e1dfb065a72329ad82cafcce4573c8d6d7a548f584bec892254181be96ddb7f43385ca4447602f9ff321a484a68fe78945743bb9a74fb40c23dfa26a01baadea1793ac3c675fb85e61229a5c670d1ed0e52e63f4a3705f04e4f3cc1f9237cb79b6494c75a2775653839d80ff11cbe15011861a85b23898c3947f9e94f355cafb515bb261274a8b6340d993c23100fb66a3f95405761b1570c7eeb35ae77f1e49b57b3500c31057ef85fef5d302ff70ba72500bfba1de984d04aa1ea481f3a828113e50ab75dca8f0f100b709065cb4a0115d07f5e5f48318a0947029d796447b906bd96c2421f128e16235dba1e1e3f1e66a89ec75d1c470a547beed37b64c5d951c5fd3e61142bf70b737b44115a3e9642c582030a5eb1f2084b23321a79d30f3b632feb683b81624970dfb66064eea5062406331411caff7f9e70271a0911ea71dc590f10aae0b77fd45beb06acd96d6ff21142091b5e880010212776afa8044c3b701617337ee114cde72232e30ede7450c5eb6848d6f62d47d4b74615c32a4a5c01ee39bb4ffc576f01c10c2284f1431901ef60ba24f3269b57017f7d30da49f5a555700b37b85fe11e80501aac88041c01b85f7f8fa76a23bd7276f85ac76f29705f6af8185e7da434355f1b82a1731377e67db5555c55ca2aa63f4ee7fc14e8d33d6a9812c97132f6da0938992953e0e8bf1f79ca92504d5c541d3deaad59341a8f28bc5d152a5f6e9f1d4e1b7e0977820851fac5a01ccb4b58536c285e4105fd587cac6ad82386d4e64521fe105c2bfa7f0eaa91593c1a59d84b556adf78a2aab739be8d0d2b70ec806cb5219e3773e369003b17272c04099b4c5cb7a76ad329f01d36ff75569450d13db312b03dafc90827e2ac255bf0fc5d17e4e3974f9e0a3b054f6bfd3432ff1559158d435649319e51fd4a6e2c82b5a1174e2ef74da9b54650088a885d702c082ad4afc65eb21be2198a85e075291aa65754c699534813ee209a0627440ae8370bbcf65721d51d4e700ef1d25718aeff0ba8473e0e44f0482eacfed0495b5aee4bf3b951558eabf6244c574c63d79de9242db6312a9bc24918099dff7d42437500e5f3e76906e86d2ac4f816183322df37af9db47acd829f75a34ee761844d7b597f9e810f2dd4c757ad672131d4641b6376e7b578476e4875de4c536e32de0d1a1c8c9665ec3d26464a8c0d26c4d3d473302e6f74f68ade6f202ec33f23e8c0498536d5146c850f23fb85aa6eb2ec063f0748593b0423aa6cf42f7c3fec3b41250b0b1817b9289357205e205dbaa8cc1186ab324dc3ac073f3ef6476b054ab45cf180cf16ec5d691cd9aecf3f6768850f33ccb111b7fb222e9946932950584877a3394974e3d2a04f142c1d6bd367b868d95d7f3f345ae02af74f79325e945454a0dfef4df2d5232110815b13a8274909f6f8cd0d05b1d75294638a2e0104e624bed96a2ab4c1aa0bbcacb23644859d77786eb24afaa2fa2149cf515469ef8161e600643e237e2114d05707711acd1550da794442699e9a1a6a255e477eb38d364c713b6a7e517b32511b461f25cfba29b3ab5b5d486bbf5184630f7e538b4b2b3a41e37294e46a11fbb29434313ab1009995426490161f63323e9725576fad0e44d8c96eeeea495c9bf44a06bc467c39";

    @Before
    public void setUp() throws Exception {
        clientC0C1 = IoBuffer.allocate(1537);
        // put the handshake type in the first position
        clientC0C1.put(RTMPConnection.RTMP_NON_ENCRYPTED);
        fillBuffer(clientC0C1, "RTMP-C0C1.dat");
        clientC2 = IoBuffer.allocate(1536);
        fillBuffer(clientC2, "RTMP-C2.dat");
        serverS0S1S2part1 = IoBuffer.allocate(1537);
        // put the handshake type in the first position
        serverS0S1S2part1.put(RTMPConnection.RTMP_NON_ENCRYPTED);
        fillBuffer(serverS0S1S2part1, "RTMP-S0S1S2-01.dat");
        serverS0S1S2part2 = IoBuffer.allocate(1536);
        fillBuffer(serverS0S1S2part2, "RTMP-S0S1S2-02.dat");
    }

    private void fillBuffer(IoBuffer buf, String byteDumpFile) throws Exception {
        File f = new File(String.format("%s/target/test-classes/%s", System.getProperty("user.dir"), byteDumpFile));
        FileInputStream fis = new FileInputStream(f);
        ByteBuffer bb = ByteBuffer.allocate((int) f.length());
        fis.getChannel().read(bb);
        bb.flip();
        buf.put(bb);
        buf.flip();
        log.debug("Filled buffer: {}", buf);
        fis.close();
    }

    @After
    public void tearDown() throws Exception {
        clientC0C1.free();
        clientC2.free();
        serverS0S1S2part1.free();
        serverS0S1S2part2.free();
    }

    @Test
    public void testClientDigest() throws InterruptedException {
        log.info("\ntestClientDigest");
        OutboundHandshake out = new OutboundHandshake();
        int algorithm = 0;
        byte[] handshakeBytes = out.getHandshakeBytes();
        // get the handshake digest
        int digestPos = out.getDigestOffset(algorithm, handshakeBytes, 0);
        log.debug("Digest position offset: {}", digestPos);
        out.calculateDigest(digestPos, handshakeBytes, 0, RTMPHandshake.GENUINE_FP_KEY, 30, handshakeBytes, digestPos);
        log.debug("Calculated digest: {}", Hex.encodeHexString(Arrays.copyOfRange(handshakeBytes, digestPos, digestPos + 32)));
        Assert.assertTrue(out.verifyDigest(digestPos, handshakeBytes, RTMPHandshake.GENUINE_FP_KEY, 30));
    }

    @Test
    public void testServerDigest() throws InterruptedException {
        log.info("\ntestServerDigest");
        InboundHandshake in = new InboundHandshake();
        int algorithm = 0;
        byte[] handshakeBytes = in.getHandshakeBytes();
        // get the handshake digest
        int digestPos = in.getDigestOffset(algorithm, handshakeBytes, 0);
        log.debug("Digest position offset: {}", digestPos);
        in.calculateDigest(digestPos, handshakeBytes, 0, RTMPHandshake.GENUINE_FMS_KEY, 36, handshakeBytes, digestPos);
        log.debug("Calculated digest: {}", Hex.encodeHexString(Arrays.copyOfRange(handshakeBytes, digestPos, digestPos + 32)));
        Assert.assertTrue(in.verifyDigest(digestPos, handshakeBytes, RTMPHandshake.GENUINE_FMS_KEY, 36));
    }

    /** Clientside test */
    @Test
    public void testOutboundHandshake() {
        log.info("\ntestOutboundHandshake");
        OutboundHandshake out = new OutboundHandshake();
        // set the handshake type
        out.setHandshakeType(RTMPConnection.RTMP_NON_ENCRYPTED);
        // called initially with null input which triggers creation of C1
        //IoBuffer C1 = hs.doHandshake(null);
        //log.debug("C1: {}", C1);
        //log.debug("C0C1 bytes: {}", new String(clientC0C1.array()));

        // strip 03 byte
        serverS0S1S2part1.get();
        // send in the first part of server handshake
        IoBuffer C2 = out.decodeServerResponse1(serverS0S1S2part1);
        Assert.assertNotNull(C2);
        log.debug("S1 (first): {}", C2);
        // send in the second part of server handshake, this creates C2
        boolean res = out.decodeServerResponse2(serverS0S1S2part2);
        Assert.assertTrue(res);
        log.debug("S2 (second): {}", res);

        //log.debug("Server bytes1: {}", new String(serverS0S1S2part1.array()));
        //log.debug("Server bytes2: {}", new String(serverS0S1S2part2.array()));

        // put parts 1 and 2 together
        IoBuffer S0S1S2 = IoBuffer.allocate(3073);
        S0S1S2.put(serverS0S1S2part1);
        S0S1S2.put(serverS0S1S2part2);
        S0S1S2.flip();
        // strip the 03 byte
        S0S1S2.get();
        // send in the combined server handshake, this creates C2
        C2 = out.doHandshake(S0S1S2);
        log.debug("C2 (third): {}", C2);

    }

    /** Serverside test */
    @Test
    public void testInboundHandshake() {
        log.info("\ntestInboundHandshake");
        InboundHandshake in = new InboundHandshake();
        // send in the first part of client handshake
        IoBuffer S1 = in.doHandshake(clientC0C1);
        log.debug("S1: {}", S1);

        OutboundHandshake out = new OutboundHandshake();
        // set the handshake type
        out.setHandshakeType(RTMPConnection.RTMP_NON_ENCRYPTED);
        IoBuffer cc = IoBuffer.allocate(1537);
        cc.put((byte) 3);
        cc.put(out.getHandshakeBytes());
        cc.flip();
        // reinstance to prevent thread local errors
        in = new InboundHandshake();
        S1 = in.doHandshake(cc);
        log.debug("S1: {}", S1);
    }

    @Test
    public void testValidate() {
        log.info("\ntestValidate");
        // server side handshake handler
        InboundHandshake in = new InboundHandshake();
        // client side handshake handler
        OutboundHandshake out = new OutboundHandshake();
        // set the handshake type
        out.setHandshakeType(RTMPConnection.RTMP_NON_ENCRYPTED);

        IoBuffer ss = IoBuffer.allocate(1536);
        ss.put(in.getHandshakeBytes());
        ss.flip();
        log.debug("Validate server: {}", ss);
        boolean generatedS1 = out.validate(ss.array());
        log.debug("Handshake is valid: {}", generatedS1);

        IoBuffer cc = IoBuffer.allocate(1536);
        cc.put(out.getHandshakeBytes());
        cc.flip();
        log.debug("Validate client: {}", cc);
        boolean generatedC1 = in.validate(cc.array());
        log.debug("Handshake is valid: {}", generatedC1);

        // try C01
        // strip the 03 type byte
        clientC0C1.get();
        log.debug("Validate client: {}", clientC0C1);
        boolean client = in.validate(clientC0C1.array());
        log.debug("Handshake is valid: {}", client);

        // try SO12
        IoBuffer S0S1S2 = IoBuffer.allocate(3073);
        S0S1S2.put(serverS0S1S2part1);
        S0S1S2.put(serverS0S1S2part2);
        S0S1S2.flip();
        // strip the 03 type byte
        S0S1S2.get();
        log.debug("Validate server: {}", S0S1S2);
        boolean server = out.validate(S0S1S2.array());
        log.debug("Handshake is valid: {}", server);

        Assert.assertTrue(generatedC1 && generatedS1 && client && server);
    }

    @Test
    public void testValidateFromBrowsers() {
        log.info("\ntestValidateFromBrowsers");
        // no handshake type bytes are included here
        
        // server side handshake handler
        InboundHandshake in = new InboundHandshake();
        // CHROME
        IoBuffer cc = IoBuffer.allocate(1536);
        cc.put(IOUtils.hexStringToByteArray(chromeFlashPlayerC0C1));
        cc.flip();
        log.debug("Validate chrome: {}", cc);
        boolean chrome = in.validate(cc.array());
        cc.clear();
        // FIREFOX
        cc.put(IOUtils.hexStringToByteArray(firefoxFlashPlayerC0C1));
        cc.flip();
        log.debug("Validate firefox: {}", cc);
        boolean firefox = in.validate(cc.array());
        cc.clear();
        // FFMPEG
        cc.put(IOUtils.hexStringToByteArray(ffmpegC0C1));
        cc.flip();
        log.debug("Validate ffmpeg: {}", cc);
        boolean ffmpeg = in.validate(cc.array());
        Assert.assertTrue(chrome && firefox && !ffmpeg);
    }

    @Test
    public void testValidateFromYouTube() {
        log.info("\ntestValidateFromYouTube");
        // client side handshake handler
        OutboundHandshake out = new OutboundHandshake();
        // server response
        IoBuffer y = IoBuffer.allocate(0);
        y.setAutoExpand(true);
        y.put(IOUtils.hexStringToByteArray(youtubeS1));
        y.flip();
        log.debug("Validate youtube: {}", y);
        boolean youtube = out.validate(y.array());
        //boolean decoded = out.decodeServerResponse1(y);
        
        //Assert.assertTrue(youtube && decoded);
    }

}
