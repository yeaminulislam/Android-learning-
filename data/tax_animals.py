# -*- coding: utf-8 -*-
"""প্রাণীজগতের পরিবার তালিকা — প্রতি লাইন: শ্রেণি|বর্গ|পরিবার(বৈজ্ঞানিক)|পরিবার(বাংলা)"""

MAMMALS = """
Mammalia|Primates|Hominidae|মানুষ ও বড় বানর
Mammalia|Primates|Cercopithecidae|পুরোনো-পৃথিবীর বাঁদর
Mammalia|Primates|Hylobatidae|গিবন
Mammalia|Primates|Cebidae|নতুন-পৃথিবীর বাঁদর
Mammalia|Primates|Callitrichidae|মারমোসেট ও টামারিন
Mammalia|Primates|Lemuridae|লেমুর
Mammalia|Primates|Lorisidae|লোরিস ও কুমকুম
Mammalia|Primates|Tarsiidae|টারসিয়ার
Mammalia|Primates|Atelidae|কীচক বাঁদর
Mammalia|Primates|Pitheciidae|উয়াকারি
Mammalia|Carnivora|Felidae|বিড়াল পরিবার
Mammalia|Carnivora|Canidae|কুকুর পরিবার
Mammalia|Carnivora|Ursidae|ভালুক পরিবার
Mammalia|Carnivora|Mustelidae|বেজি-ওটার পরিবার
Mammalia|Carnivora|Viverridae|খটাশ পরিবার
Mammalia|Carnivora|Herpestidae|নেউল পরিবার
Mammalia|Carnivora|Hyaenidae|হায়েনা পরিবার
Mammalia|Carnivora|Phocidae|কানবিহীন সিল
Mammalia|Carnivora|Otariidae|সিংহাসিল ও ফারসিল
Mammalia|Carnivora|Odobenidae|ওয়ালরাস
Mammalia|Carnivora|Procyonidae|র‍্যাকুন পরিবার
Mammalia|Carnivora|Ailuridae|লাল পান্ডা
Mammalia|Carnivora|Eupleridae|মাদাগাস্কার মাংসাশী
Mammalia|Carnivora|Prionodontidae|এশীয় লিনসাং
Mammalia|Carnivora|Mephitidae|স্কাংক পরিবার
Mammalia|Rodentia|Muridae|ইঁদুর ও মাউস
Mammalia|Rodentia|Cricetidae|হ্যামস্টার ও ভোল
Mammalia|Rodentia|Sciuridae|কাঠবিড়ালি
Mammalia|Rodentia|Hystricidae|সজারু
Mammalia|Rodentia|Caviidae|গিনিপিগ ও ক্যাপিবারা
Mammalia|Rodentia|Dipodidae|জার্বোয়া
Mammalia|Rodentia|Geomyidae|পকেট গোফার
Mammalia|Rodentia|Bathyergidae|আফ্রিকান মোল-ইঁদুর
Mammalia|Rodentia|Capromyidae|হুটিয়া
Mammalia|Rodentia|Spalacidae|বাঁশ-ইঁদুর
Mammalia|Rodentia|Anomaluridae|উড়ন্ত স্কোয়ারেল
Mammalia|Rodentia|Octodontidae|দেগু
Mammalia|Rodentia|Castoridae|বিভার
Mammalia|Rodentia|Heteromyidae|ক্যাঙ্গারু ইঁদুর
Mammalia|Chiroptera|Pteropodidae|ফলখেকো বাদুড়
Mammalia|Chiroptera|Vespertilionidae|সাধারণ বাদুড়
Mammalia|Chiroptera|Rhinolophidae|নালমুখী বাদুড়
Mammalia|Chiroptera|Hipposideridae|পাতামুখী বাদুড়
Mammalia|Chiroptera|Megadermatidae|মিথ্যা ভ্যাম্পায়ার বাদুড়
Mammalia|Chiroptera|Phyllostomidae|পাতানাকি বাদুড়
Mammalia|Chiroptera|Molossidae|মুক্ত-লেজ বাদুড়
Mammalia|Chiroptera|Emballonuridae|থলে-লেজ বাদুড়
Mammalia|Chiroptera|Nycteridae|ফাটামুখী বাদুড়
Mammalia|Chiroptera|Miniopteridae|লম্বা ডানার বাদুড়
Mammalia|Artiodactyla|Bovidae|গরু-ছাগল-অ্যান্টিলোপ
Mammalia|Artiodactyla|Cervidae|হরিণ পরিবার
Mammalia|Artiodactyla|Suidae|শুয়োর পরিবার
Mammalia|Artiodactyla|Camelidae|উট পরিবার
Mammalia|Artiodactyla|Giraffidae|জিরাফ পরিবার
Mammalia|Artiodactyla|Hippopotamidae|জলহস্তী পরিবার
Mammalia|Artiodactyla|Tragulidae|মাউস ডিয়ার
Mammalia|Artiodactyla|Moschidae|কস্তুরী হরিণ
Mammalia|Artiodactyla|Antilocapridae|প্রংহর্ন
Mammalia|Artiodactyla|Tayassuidae|পেকারি
Mammalia|Perissodactyla|Equidae|ঘোড়া পরিবার
Mammalia|Perissodactyla|Rhinocerotidae|গণ্ডার পরিবার
Mammalia|Perissodactyla|Tapiridae|তাপির পরিবার
Mammalia|Proboscidea|Elephantidae|হাতি পরিবার
Mammalia|Proboscidea|Mammutidae|ম্যামুট পরিবার
Mammalia|Proboscidea|Gomphotheriidae|গোম্ফোথেরিয়াম
Mammalia|Cetacea|Delphinidae|ডলফিন পরিবার
Mammalia|Cetacea|Balaenopteridae|রোরকোয়াল তিমি
Mammalia|Cetacea|Physeteridae|স্পার্ম তিমি
Mammalia|Cetacea|Monodontidae|বেলুগা ও নারওয়াল
Mammalia|Cetacea|Ziphiidae|ঠোঁটযুক্ত তিমি
Mammalia|Cetacea|Phocoenidae|পোরপয়েজ
Mammalia|Cetacea|Platanistidae|গাঙ্গেয় ডলফিন
Mammalia|Cetacea|Iniidae|আমাজন ডলফিন
Mammalia|Cetacea|Eschrichtiidae|ধূসর তিমি
Mammalia|Cetacea|Kogiidae|বামন তিমি
Mammalia|Cetacea|Balaenidae|ডান তিমি
Mammalia|Sirenia|Dugongidae|ডুয়ং
Mammalia|Sirenia|Trichechidae|মানাটি
Mammalia|Pholidota|Manidae|পাঙলিন পরিবার
Mammalia|Lagomorpha|Leporidae|খরগোশ ও শশক
Mammalia|Lagomorpha|Ochotonidae|পিকা
Mammalia|Eulipotyphla|Erinaceidae|কাঁটাচুঁচা
Mammalia|Eulipotyphla|Soricidae|ছুঁচো
Mammalia|Eulipotyphla|Talpidae|মোল
Mammalia|Eulipotyphla|Solennodontidae|সোলেনোডন
Mammalia|Eulipotyphla|Tenrecidae|টেনরেক
Mammalia|Diprotodontia|Macropodidae|ক্যাঙ্গারু ও ওয়ালাবি
Mammalia|Diprotodontia|Phascolarctidae|কোয়ালা
Mammalia|Diprotodontia|Phalangeridae|কুসকুস
Mammalia|Diprotodontia|Petauridae|গ্লাইডার পসাম
Mammalia|Diprotodontia|Vombatidae|ওম্বাট
Mammalia|Diprotodontia|Potoroidae|পটোরু
Mammalia|Diprotodontia|Tarsipedidae|মধু পসাম
Mammalia|Diprotodontia|Acrobatidae|ফেদারটেইল পসাম
Mammalia|Marsupialia|Dasyuridae|কোয়ল ও টাসমেনিয়ান ডেভিল
Mammalia|Marsupialia|Didelphidae|অপসাম
Mammalia|Marsupialia|Myrmecobiidae|নাম্বাট
Mammalia|Marsupialia|Peramelidae|বিলবি
Mammalia|Marsupialia|Notoryctidae|মরু-মোল
Mammalia|Marsupialia|Thylacinidae|থিলাসিন
Mammalia|Monotremata|Ornithorhynchidae|প্লাটিপাস
Mammalia|Monotremata|Tachyglossidae|ইচিডনা
"""

BIRDS = """
Aves|Passeriformes|Passeridae|চড়ুই পরিবার
Aves|Passeriformes|Corvidae|কাক পরিবার
Aves|Passeriformes|Pycnonotidae|বুলবুলি
Aves|Passeriformes|Muscicapidae|মাছিধরা পাখি
Aves|Passeriformes|Sylviidae|সিলভিয়া ওয়ার্বলার
Aves|Passeriformes|Turdidae|থ্রাশ
Aves|Passeriformes|Hirundinidae|সোয়ালো-মার্টিন
Aves|Passeriformes|Motacillidae|ওয়াগটেইল
Aves|Passeriformes|Alaudidae|লার্ক
Aves|Passeriformes|Fringillidae|ফিঞ্চ
Aves|Passeriformes|Estrildidae|মুনিয়া-মানিকজোড়
Aves|Passeriformes|Sturnidae|শালিক
Aves|Passeriformes|Dicruridae|ফিঙে
Aves|Passeriformes|Ploceidae|বুনা পাখি
Aves|Passeriformes|Campephagidae|ক্যাটারপিলার-ইটার
Aves|Passeriformes|Oriolidae|হলদেপাখা
Aves|Passeriformes|Vangidae|ভাঙা
Aves|Passeriformes|Leiothrichidae|লাফিংথ্রাশ
Aves|Passeriformes|Paradoxornithidae|প্যারাডক্সর্নিস
Aves|Passeriformes|Chloropseidae|লিফবার্ড
Aves|Passeriformes|Prunellidae|অ্যাকসেন্টর
Aves|Passeriformes|Paridae|টিটমাউস
Aves|Passeriformes|Sittidae|নিথ্যাচ
Aves|Passeriformes|Troglodytidae|রেন
Aves|Passeriformes|Thraupidae|ট্যানাজার
Aves|Passeriformes|Icteridae|ব্ল্যাকবার্ড
Aves|Passeriformes|Parulidae|নিউ-ওয়ার্ল্ড ওয়ার্বলার
Aves|Passeriformes|Tyrannidae|টাইরান্ট ফ্লাইক্যাচার
Aves|Passeriformes|Nectariniidae|সূর্যপাখি
Aves|Passeriformes|Dicaeidae|ফুলখাইয়ে
Aves|Passeriformes|Zosteropidae|হোয়াইট-আই
Aves|Passeriformes|Timaliidae|বাবলার
Aves|Passeriformes|Aegithinidae|আইওরা
Aves|Passeriformes|Artamidae|উডসোয়ালো
Aves|Passeriformes|Pittidae|পিতা
Aves|Passeriformes|Eurylaimidae|ব্রডবিল
Aves|Passeriformes|Cisticolidae|সিস্টিকোলা
Aves|Passeriformes|Monarchidae|মোনার্ক ফ্লাইক্যাচার
Aves|Passeriformes|Regulidae|কিংলেট
Aves|Passeriformes|Bombycillidae|ওয়াক্সউইং
Aves|Passeriformes|Laniidae|শ্রাইক
Aves|Accipitriformes|Accipitridae|বাজ-ঈগল-চিল
Aves|Accipitriformes|Pandionidae|মাছরাঙা ঈগল
Aves|Accipitriformes|Sagittariidae|সেক্রেটারি বার্ড
Aves|Accipitriformes|Cathartidae|শকুন
Aves|Falconiformes|Falconidae|ফ্যালকন পরিবার
Aves|Anseriformes|Anatidae|হাঁস-রাজহাঁস-চখাচখি
Aves|Anseriformes|Anhimidae|স্ক্রিমার
Aves|Galliformes|Phasianidae|তীতির-মোরগ
Aves|Galliformes|Numididae|গিনিফাউল
Aves|Galliformes|Odontophoridae|কুয়েইল
Aves|Galliformes|Megapodiidae|মেগাপোড
Aves|Galliformes|Cracidae|কুরাসো
Aves|Columbiformes|Columbidae|কবুতর-ঘুঘু
Aves|Psittaciformes|Psittaculidae|টিয়া পরিবার
Aves|Psittaciformes|Psittacidae|ম্যাকাও-আমাজন
Aves|Psittaciformes|Cacatuidae|কোকাটু
Aves|Piciformes|Picidae|কাঠঠোকরা
Aves|Piciformes|Megalaimidae|বারবেট
Aves|Piciformes|Ramphastidae|টুকান
Aves|Piciformes|Bucconidae|পাফবার্ড
Aves|Piciformes|Capitonidae|ক্যাপিটো
Aves|Piciformes|Indicatoridae|হানিগাইড
Aves|Strigiformes|Strigidae|পেঁচা পরিবার
Aves|Strigiformes|Tytonidae|ঘাসফুল পেঁচা
Aves|Charadriiformes|Laridae|সিগাল-টার্ন
Aves|Charadriiformes|Charadriidae|প্লোভার
Aves|Charadriiformes|Scolopacidae|স্নাইপ-স্যান্ডপাইপার
Aves|Charadriiformes|Rynchopidae|স্কিমার
Aves|Charadriiformes|Glareolidae|প্র্যাটিনকোল
Aves|Charadriiformes|Jacanidae|জলকুমুদ পাখি
Aves|Charadriiformes|Rostratulidae|রঙিন স্নাইপ
Aves|Charadriiformes|Alcidae|অক
Aves|Charadriiformes|Stercorariidae|স্কুয়া
Aves|Charadriiformes|Haematopodidae|অয়েস্টারক্যাচার
Aves|Charadriiformes|Recurvirostridae|স্টিল্ট-অ্যাভোসেট
Aves|Charadriiformes|Thinocoridae|সীডস্নাইপ
Aves|Charadriiformes|Turnicidae|বাটান-তীতির
Aves|Pelecaniformes|Ardeidae|বক পরিবার
Aves|Pelecaniformes|Threskiornithidae|আইবিস-হাঁসপাখা
Aves|Pelecaniformes|Pelecanidae|পেলিক্যান
Aves|Pelecaniformes|Balaenicipitidae|শু-বিল
Aves|Pelecaniformes|Scopidae|হ্যামারকপ
Aves|Suliformes|Phalacrocoracidae|করমোরান্ট
Aves|Suliformes|Fregatidae|ফ্রিগেটবার্ড
Aves|Suliformes|Sulidae|গানেট-বুবি
Aves|Suliformes|Anhingidae|ডার্টার
Aves|Gruiformes|Rallidae|রেল-ক্রেইক
Aves|Gruiformes|Gruidae|সারস
Aves|Gruiformes|Heliornithidae|ফিনফুট
Aves|Apodiformes|Apodidae|সুইফট
Aves|Apodiformes|Hemiprocnidae|বৃক্ষ-সুইফট
Aves|Apodiformes|Trochilidae|হামিংবার্ড
Aves|Caprimulgiformes|Caprimulgidae|জারিপি
Aves|Caprimulgiformes|Nyctibiidae|পটু
Aves|Caprimulgiformes|Aegothelidae|ময়লা-পেঁচা
Aves|Caprimulgiformes|Steatornithidae|তেলপাখা
Aves|Coraciiformes|Alcedinidae|মাছরাঙা
Aves|Coraciiformes|Meropidae|বি ইটার
Aves|Coraciiformes|Coraciidae|রোলার
Aves|Coraciiformes|Halcyonidae|বনের মাছরাঙা
Aves|Coraciiformes|Todidae|টডি
Aves|Coraciiformes|Momotidae|মোটমোট
Aves|Bucerotiformes|Bucerotidae|ধনেশ
Aves|Bucerotiformes|Upupidae|হুডহুড
Aves|Bucerotiformes|Phoeniculidae|উডহুপো
Aves|Trogoniformes|Trogonidae|ট্রোগন-কেতজাল
Aves|Procellariiformes|Procellariidae|শিয়ারওয়াটার
Aves|Procellariiformes|Diomedeidae|আলবাট্রস
Aves|Procellariiformes|Hydrobatidae|স্টর্ম-পেট্রেল
Aves|Procellariiformes|Pelecanoididae|ডাইভিং-পেট্রেল
Aves|Podicipediformes|Podicipedidae|গ্রিব
Aves|Gaviiformes|Gaviidae|লুন
Aves|Otidiformes|Otididae|বাটান
Aves|Otidiformes|Mesitornithidae|মেসাইট
Aves|Eurypygiformes|Eurypygidae|সানবিটার্ন
Aves|Cuculiformes|Cuculidae|কোকিল
Aves|Phaethontiformes|Phaethontidae|ট্রপিকবার্ড
Aves|Opisthocomiformes|Opisthocomidae|হোয়াটজিন
Aves|Ciconiiformes|Ciconiidae|সারস-কলকলে
Aves|Apterygiformes|Apterygidae|কিউই
Aves|Dinornithiformes|Dinornithidae|মোয়া
Aves|Sphenisciformes|Spheniscidae|পেঙ্গুইন
"""

REPTILES = """
Reptilia|Squamata|Colubridae|নিরীহ সাপ
Reptilia|Squamata|Viperidae|বিষধর ভাইপার
Reptilia|Squamata|Elapidae|কোবরা ও করাল সাপ
Reptilia|Squamata|Pythonidae|অজগর
Reptilia|Squamata|Boidae|বোয়া সাপ
Reptilia|Squamata|Gekkonidae|টিকটিকি
Reptilia|Squamata|Agamidae|গিরগিটি
Reptilia|Squamata|Iguanidae|ইগুয়ানা
Reptilia|Squamata|Scincidae|চোখা সাপ
Reptilia|Squamata|Varanidae|গোহ
Reptilia|Squamata|Chamaeleonidae|গিরগিট
Reptilia|Squamata|Lacertidae|প্রাচীর টিকটিকি
Reptilia|Squamata|Amphisbaenidae|কেঁচো-গিরগিটি
Reptilia|Squamata|Cordylidae|গার্ডেলড লিজার্ড
Reptilia|Squamata|Teiidae|হুইপটেইল
Reptilia|Squamata|Anguidae|গ্লাস লিজার্ড
Reptilia|Squamata|Dibamidae|অন্ধ লিজার্ড
Reptilia|Squamata|Xenosauridae|নব-সরীসৃপ
Reptilia|Squamata|Leptotyphlopidae|সুতো সাপ
Reptilia|Squamata|Typhlopidae|অন্ধ সাপ
Reptilia|Squamata|Acrochordidae|বর্ষা সাপ
Reptilia|Squamata|Homalopsidae|কাদা সাপ
Reptilia|Squamata|Pareatidae|শামুকখেকো সাপ
Reptilia|Squamata|Natricidae|জলচর নিরীহ সাপ
Reptilia|Squamata|Xenodermatidae|অদ্ভুত-চামড়ার সাপ
Reptilia|Squamata|Helodermatidae|বিষধর টিকটিকি
Reptilia|Testudines|Testudinidae|স্থল কচ্ছপ
Reptilia|Testudines|Emydidae|মিঠাপানির কছিম
Reptilia|Testudines|Geoemydidae|এশীয় কছিম
Reptilia|Testudines|Trionychidae|নরম খোলস কছিম
Reptilia|Testudines|Cheloniidae|সবুজ সমুদ্র কচ্ছপ
Reptilia|Testudines|Dermochelyidae|চামড়ার কচ্ছপ
Reptilia|Testudines|Chelydridae|স্ন্যাপিং কচ্ছপ
Reptilia|Testudines|Kinosternidae|কাদা কচ্ছপ
Reptilia|Testudines|Platysternidae|বড়মাথা কছিম
Reptilia|Testudines|Bataguridae|নদী কছিম
Reptilia|Crocodilia|Crocodylidae|কুমির
Reptilia|Crocodilia|Alligatoridae|অ্যালিগেটর ও কায়মান
Reptilia|Crocodilia|Gavialidae|ঘড়িয়াল
Reptilia|Crocodilia|Tomistomidae|ভুঁই-ঘড়িয়াল
Reptilia|Rhynchocephalia|Sphenodontidae|তুয়াতারা
"""

AMPHIBIANS = """
Amphibia|Anura|Ranidae|ব্যাঙ
Amphibia|Anura|Bufonidae|বেঙ
Amphibia|Anura|Dicroglossidae|এশীয় ব্যাঙ
Amphibia|Anura|Microhylidae|সরুমুখো ব্যাঙ
Amphibia|Anura|Rhacophoridae|উড়ন্ত ব্যাঙ
Amphibia|Anura|Hylidae|বৃক্ষব্যাঙ
Amphibia|Anura|Megophryidae|পাতা-সাঁট ব্যাঙ
Amphibia|Anura|Bombinatoridae|জল বেরঙ
Amphibia|Anura|Arthroleptidae|চিৎকার ব্যাঙ
Amphibia|Anura|Mantellidae|মাদাগাস্কার ব্যাঙ
Amphibia|Anura|Dendrobatidae|বিষাক্ত তীর ব্যাঙ
Amphibia|Anura|Ceratophryidae|শিং ব্যাঙ
Amphibia|Anura|Nyctibatrachidae|রাত ব্যাঙ
Amphibia|Anura|Ranixalidae|ভারতীয় ঝর্ণা ব্যাঙ
Amphibia|Anura|Pipidae|আফ্রিকান নখর ব্যাঙ
Amphibia|Anura|Pelobatidae|রসুন ব্যাঙ
Amphibia|Anura|Hyperoliidae|রিড ব্যাঙ
Amphibia|Anura|Centrolenidae|কাচ ব্যাঙ
Amphibia|Anura|Leptodactylidae|দক্ষিণ আমেরিকান ব্যাঙ
Amphibia|Anura|Hylodinae_fam|ঝর্ণা ব্যাঙ
Amphibia|Anura|Allophryninae_fam|গায়ানা ব্যাঙ
Amphibia|Anura|Odontobatrachinae_fam|দাঁতি ব্যাঙ
Amphibia|Caudata|Salamandridae|নিউট ও স্যালাম্যান্ডার
Amphibia|Caudata|Cryptobranchidae|দৈত্য স্যালাম্যান্ডার
Amphibia|Caudata|Ambystomatidae|মোল স্যালাম্যান্ডার
Amphibia|Caudata|Plethodontidae|ফুসফুসবিহীন স্যালাম্যান্ডার
Amphibia|Caudata|Hynobiidae|এশীয় স্যালাম্যান্ডার
Amphibia|Caudata|Proteidae|অল্ম ও মুডপপি
Amphibia|Caudata|Sirenidae|সাইরেন স্যালাম্যান্ডার
Amphibia|Caudata|Amphiuma_fam|কংগ্রি ইল স্যালাম্যান্ডার
Amphibia|Gymnophiona|Caeciliidae|সিসিলিয়ান
Amphibia|Gymnophiona|Ichthyophiidae|এশীয় সিসিলিয়ান
Amphibia|Gymnophiona|Siphonopinae_fam|মাটি সিসিলিয়ান
"""

FISHES = """
Actinopterygii|Cypriniformes|Cyprinidae|কার্প পরিবার
Actinopterygii|Cypriniformes|Cobitidae|লোচ
Actinopterygii|Cypriniformes|Balitoridae|পাথর-লোচ
Actinopterygii|Cypriniformes|Nemacheilidae|স্টোন লোচ
Actinopterygii|Cypriniformes|Psilorhynchidae|ঝর্ণা-চুষা মাছ
Actinopterygii|Cypriniformes|Sundadanionidae|সুন্দাডানিও
Actinopterygii|Cypriniformes|Leuciscinae_fam|মিনো
Actinopterygii|Cypriniformes|Gobioninae_fam|গবিওন
Actinopterygii|Siluriformes|Bagridae|বাঘাইর
Actinopterygii|Siluriformes|Schilbeidae|শিলবি
Actinopterygii|Siluriformes|Clariidae|মাগুর
Actinopterygii|Siluriformes|Sisoridae|পাহাড়ি চুঁই
Actinopterygii|Siluriformes|Pangasiidae|পাঙ্গাস
Actinopterygii|Siluriformes|Mochokidae|কুইকুই
Actinopterygii|Siluriformes|Loricariidae|সাকারমাউথ
Actinopterygii|Siluriformes|Callichthyidae|করিডোরাস
Actinopterygii|Siluriformes|Ariidae|সামুদ্রিক ক্যাটফিশ
Actinopterygii|Siluriformes|Heteropneustidae|শিং মাছ
Actinopterygii|Siluriformes|Olyridae|দীর্ঘচুঁই
Actinopterygii|Siluriformes|Akysidae|একিস ক্যাটফিশ
Actinopterygii|Siluriformes|Erethistidae|এরেথিস্ট
Actinopterygii|Siluriformes|Amblycipitinae_fam|ঝর্ণা ক্যাটফিশ
Actinopterygii|Siluriformes|Pimelodinae_fam|দীর্ঘশুঁড় ক্যাটফিশ
Actinopterygii|Perciformes|Cichlidae|সিচলিড
Actinopterygii|Perciformes|Anabantidae|কৈ মাছ
Actinopterygii|Perciformes|Osphronemidae|গুরামি
Actinopterygii|Perciformes|Channidae|শোল মাছ
Actinopterygii|Perciformes|Badidae|বাদিস
Actinopterygii|Perciformes|Nandidae|নান্দুস
Actinopterygii|Perciformes|Serranidae|গ্রুপার
Actinopterygii|Perciformes|Carangidae|ট্রাভালি
Actinopterygii|Perciformes|Lutjanidae|স্ন্যাপার
Actinopterygii|Perciformes|Scaridae|তোতা মাছ
Actinopterygii|Perciformes|Chaetodontidae|বাটারফ্লাই মাছ
Actinopterygii|Perciformes|Pomacentridae|ড্যামসেল
Actinopterygii|Perciformes|Acanthuridae|সার্জন মাছ
Actinopterygii|Perciformes|Mullidae|গোয়াত মাছ
Actinopterygii|Perciformes|Sparidae|ব্রেম
Actinopterygii|Perciformes|Sciaenidae|ড্রাম মাছ
Actinopterygii|Perciformes|Labridae|রাস মাছ
Actinopterygii|Perciformes|Gobiidae|গবি
Actinopterygii|Perciformes|Eleotridae|ডরমিটর
Actinopterygii|Perciformes|Apogonidae|কার্ডিনাল মাছ
Actinopterygii|Perciformes|Monodactylinae_fam|মোনোড্যাকটাইল
Actinopterygii|Perciformes|Toxotinae_fam|তীরন্দাজ মাছ
Actinopterygii|Perciformes|Notothenioideinae_fam|বরফ মাছ
Actinopterygii|Clupeiformes|Clupeidae|হেরিং-ইলিশ
Actinopterygii|Clupeiformes|Engraulidae|অ্যাঙ্কোভি
Actinopterygii|Clupeiformes|Pristigasterinae_fam|লংফিন হেরিং
Actinopterygii|Salmoniformes|Salmonidae|স্যামন-ট্রাউট
Actinopterygii|Anguilliformes|Anguillidae|ইল
Actinopterygii|Anguilliformes|Muraenesocinae_fam|পাইক-কংগার
Actinopterygii|Anguilliformes|Muraenidae|মুরেনা
Actinopterygii|Anguilliformes|Congrinae_fam|কংগার ইল
Actinopterygii|Characiformes|Characidae|টেট্রা
Actinopterygii|Characiformes|Citharininae_fam|সিথারিন
Actinopterygii|Characiformes|Hemiodontinae_fam|হেমিওডন
Actinopterygii|Characiformes|Alestinae_fam|আফ্রিকান টেট্রা
Actinopterygii|Cyprinodontiformes|Poeciliidae|গাপি-মলি
Actinopterygii|Cyprinodontiformes|Aplocheilinae_fam|কিলিফিশ
Actinopterygii|Cyprinodontiformes|Nothobranchiinae_fam|নথোব্রাঞ্চি
Actinopterygii|Cyprinodontiformes|Rivulinae_fam|রিভিউল
Actinopterygii|Cyprinodontiformes|Fundulinae_fam|টপমিনো
Actinopterygii|Atheriniformes|Atherininae_fam|সিলভারসাইড
Actinopterygii|Atheriniformes|Melanotaeniinae_fam|রেইনবো ফিশ
Actinopterygii|Beloniformes|Beloninae_fam|গরমাছ
Actinopterygii|Beloniformes|Exocoetinae_fam|উড়ন্ত মাছ
Actinopterygii|Beloniformes|Hemiramphinae_fam|হাফবিক
Actinopterygii|Tetraodontiformes|Tetraodontinae_fam|পাফার
Actinopterygii|Tetraodontiformes|Diodontinae_fam|কাঁটা পাফার
Actinopterygii|Tetraodontiformes|Molinae_fam|সানফিশ
Actinopterygii|Tetraodontiformes|Balistinae_fam|ট্রিগার ফিশ
Actinopterygii|Tetraodontiformes|Ostraciinae_fam|বাক্স মাছ
Actinopterygii|Gadiformes|Gadinae_fam|কড
Actinopterygii|Gadiformes|Morinae_fam|মোরা
Actinopterygii|Gadiformes|Macrourinae_fam|রেটটেইল
Actinopterygii|Scorpaeniformes|Scorpaeninae_fam|বিচ্ছু মাছ
Actinopterygii|Scorpaeniformes|Cottinae_fam|স্কালপিন
Actinopterygii|Scorpaeniformes|Platycephalinae_fam|ফ্ল্যাটহেড
Actinopterygii|Scorpaeniformes|Psychrolutinae_fam|ব্লবফিশ
Actinopterygii|Acipenseriformes|Acipenserinae_fam|স্টার্জন
Actinopterygii|Acipenseriformes|Polyodontinae_fam|প্যাডলফিশ
Actinopterygii|Osteoglossiformes|Osteoglossinae_fam|অ্যারোয়ানা
Actinopterygii|Osteoglossiformes|Notopterinae_fam|চাঁদা মাছ
Actinopterygii|Osteoglossiformes|Mormyrinae_fam|হাতিমাছ
Actinopterygii|Petromyzontiformes|Petromyzontinae_fam|ল্যাম্প্রে
Actinopterygii|Myxiniformes|Myxininae_fam|হ্যাগফিশ
Actinopterygii|Coelacanthiformes|Latimeriinae_fam|সিলাক্যান্থ
Actinopterygii|Lepidosireniformes|Lepidosireninae_fam|দক্ষিণ-আমেরিকান লাংফিশ
Actinopterygii|Lepidosireniformes|Protopterinae_fam|আফ্রিকান লাংফিশ
Actinopterygii|Ceratodontiformes|Ceratodontinae_fam|অস্ট্রেলীয় লাংফিশ
Actinopterygii|Amiiformes|Amiinae_fam|বোউফিন
Actinopterygii|Carcharhiniformes|Carcharhininae_fam|রিকুয়েম শার্ক
Actinopterygii|Carcharhiniformes|Triakinae_fam|হাউন্ডশার্ক
Actinopterygii|Carcharhiniformes|Scyliorhininae_fam|ক্যাটশার্ক
Actinopterygii|Carcharhiniformes|Sphyrninae_fam|হাতুড়ি শার্ক
Actinopterygii|Lamniformes|Lamninae_fam|ম্যাকারেল শার্ক
Actinopterygii|Lamniformes|Alopiinae_fam|থেসার শার্ক
Actinopterygii|Lamniformes|Cetorhininae_fam|বাস্কিং শার্ক
Actinopterygii|Lamniformes|Odontaspidinae_fam|স্যান্ড টাইগার শার্ক
Actinopterygii|Orectolobiformes|Rhincodontinae_fam|হাঙর মাছ
Actinopterygii|Orectolobiformes|Ginglymostomatinae_fam|নার্স শার্ক
Actinopterygii|Orectolobiformes|Stegostomatinae_fam|জেব্রা শার্ক
Actinopterygii|Rajiformes|Dasyatinae_fam|স্টিং রে
Actinopterygii|Rajiformes|Rajinae_fam|স্কেট
Actinopterygii|Rajiformes|Potamotrygoninae_fam|নদী রে
Actinopterygii|Rajiformes|Myliobatinae_fam|ঈগল রে
Actinopterygii|Rajiformes|Gymnurinae_fam|বাটারফ্লাই রে
Actinopterygii|Torpediniformes|Torpedininae_fam|টর্পেডো রে
Actinopterygii|Heterodontiformes|Heterodontinae_fam|বুলহেড শার্ক
Actinopterygii|Hexanchiformes|Hexanchinae_fam|ছয়-ফুলি শার্ক
Actinopterygii|Chimaeriformes|Chimaerinae_fam|ঘোস্ট শার্ক
Actinopterygii|Pristiformes|Pristinae_fam|করাত মাছ
Actinopterygii|Zeiformes|Zeinae_fam|জন ডোরি
Actinopterygii|Percopsiformes|Percopsinae_fam|ট্রাউট-পার্চ
Actinopterygii|Lampriformes|Lamprinae_fam|অপা মাছ
Actinopterygii|Beryciformes|Berycinae_fam|স্লোপ মাছ
Actinopterygii|Holocentrinae_ord|Holocentrinae_fam|স্কোয়ারেল মাছ
Actinopterygii|Perciformes|Ambassidae|চান্দা মাছ
"""

INVERTS = """
Insecta|Lepidoptera|Nymphalidae|পুঁটি প্রজাপতি
Insecta|Lepidoptera|Papilionidae|মোরন প্রজাপতি
Insecta|Lepidoptera|Pieridae|সাদা-হলুদ প্রজাপতি
Insecta|Lepidoptera|Lycaenidae|ছোট নীল প্রজাপতি
Insecta|Lepidoptera|Hesperiidae|স্কিপার
Insecta|Lepidoptera|Riodininae_fam|মেটালমার্ক
Insecta|Lepidoptera|Saturniinae_fam|রেশমি মথ
Insecta|Lepidoptera|Sphinginae_fam|হক মথ
Insecta|Lepidoptera|Noctuinae_fam|আঁধার মথ
Insecta|Lepidoptera|Geometrinae_fam|লুপার মথ
Insecta|Lepidoptera|Pyralinae_fam|ঘাস মথ
Insecta|Lepidoptera|Tortricinae_fam|পাতামোড়া মথ
Insecta|Lepidoptera|Erebinae_fam|টাইগার মথ
Insecta|Lepidoptera|Bombycinae_fam|রেশম মথ
Insecta|Lepidoptera|Zygaeninae_fam|বার্নেট মথ
Insecta|Lepidoptera|Arctiinae_fam|উলি মথ
Insecta|Lepidoptera|Lasiocampinae_fam|তাঁবু মথ
Insecta|Lepidoptera|Notodontinae_fam|প্রমিনেন্ট মথ
Insecta|Lepidoptera|Gracillariinae_fam|পাতাখনি মথ
Insecta|Lepidoptera|Pterophorinae_fam|পালক মথ
Insecta|Coleoptera|Cerambycinae_fam|দীর্ঘশৃঙ্গী বিটল
Insecta|Coleoptera|Scarabaeinae_fam|গুবরে পোকা
Insecta|Coleoptera|Curculioninae_fam|উইভিল
Insecta|Coleoptera|Chrysomelinae_fam|পাতা বিটল
Insecta|Coleoptera|Carabinae_fam|গ্রাউন্ড বিটল
Insecta|Coleoptera|Staphylininae_fam|রোভ বিটল
Insecta|Coleoptera|Lucaninae_fam|স্ট্যাগ বিটল
Insecta|Coleoptera|Buprestinae_fam|জুয়েল বিটল
Insecta|Coleoptera|Coccinellinae_fam|লেডিবার্ড
Insecta|Coleoptera|Elaterinae_fam|ক্লিক বিটল
Insecta|Coleoptera|Tenebrioninae_fam|ডার্কলিং বিটল
Insecta|Coleoptera|Hydrophilinae_fam|জল বিটল
Insecta|Coleoptera|Dytiscinae_fam|ডুবুরি বিটল
Insecta|Coleoptera|Lampyrinae_fam|জোনাকি
Insecta|Coleoptera|Meloinae_fam|ব্লিস্টার বিটল
Insecta|Coleoptera|Attelabinae_fam|পাতামোড়া উইভিল
Insecta|Coleoptera|Bostrichinae_fam|বোরার বিটল
Insecta|Coleoptera|Anobiinae_fam|কাঠ বিটল
Insecta|Coleoptera|Silphinae_fam|ক্যারিয়ন বিটল
Insecta|Coleoptera|Mordellinae_fam|টাম্বলিং বিটল
Insecta|Coleoptera|Alleculinae_fam|ছত্রাক বিটল
Insecta|Coleoptera|Chrysolina_fam|রঙিন পাতা বিটল
Insecta|Hymenoptera|Formicinae_fam|পিঁপড়া
Insecta|Hymenoptera|Myrmicinae_fam|দাঁতযুক্ত পিঁপড়া
Insecta|Hymenoptera|Apinae_fam|মৌমাছি
Insecta|Hymenoptera|Vespinae_fam|বোলতা
Insecta|Hymenoptera|Ichneumoninae_fam|ইচনিউমন বোলতা
Insecta|Hymenoptera|Braconinae_fam|ব্রাকন
Insecta|Hymenoptera|Halictinae_fam|ঘাম মৌমাছি
Insecta|Hymenoptera|Megachilinae_fam|পাতা-কাটা মৌমাছি
Insecta|Hymenoptera|Sphecinae_fam|কাঠি বোলতা
Insecta|Hymenoptera|Andreninae_fam|খনি মৌমাছি
Insecta|Hymenoptera|Tenthredininae_fam|সফ্লাই
Insecta|Hymenoptera|Chrysidinae_fam|কুকু বোলতা
Insecta|Hymenoptera|Mutillinae_fam|ভেলভেট পিঁপড়া
Insecta|Hymenoptera|Scoliinae_fam|স্কোলিয়া বোলতা
Insecta|Hymenoptera|Pompilinae_fam|মাকড়সা বোলতা
Insecta|Hymenoptera|Cynipinae_fam|গল বোলতা
Insecta|Hymenoptera|Oecophyllinae_fam|তাঁতি পিঁপড়া
Insecta|Diptera|Culicinae_fam|মশা
Insecta|Diptera|Muscinae_fam|মাছি
Insecta|Diptera|Calliphorinae_fam|নীল মাছি
Insecta|Diptera|Syrphinae_fam|ফুল মাছি
Insecta|Diptera|Tephritinae_fam|ফল মাছি
Insecta|Diptera|Tipulinae_fam|ক্রেইন ফ্লাই
Insecta|Diptera|Tabaninae_fam|ডাঁশ
Insecta|Diptera|Drosophilinae_fam|ছোট ফল মাছি
Insecta|Diptera|Psychodinae_fam|মথ মাছি
Insecta|Diptera|Cecidomyiinae_fam|গল মাছি
Insecta|Diptera|Asilinae_fam|ডাকাত মাছি
Insecta|Diptera|Bombyliinae_fam|ভ্রমর মাছি
Insecta|Diptera|Hippoboscinae_fam|কেনা মাছি
Insecta|Diptera|Simuliinae_fam|কালো মাছি
Insecta|Diptera|Ceratopogoninae_fam|নো-সি মাছি
Insecta|Diptera|Stratiomyinae_fam|সৈনিক মাছি
Insecta|Diptera|Rhagioninae_fam|স্নাইপ মাছি
Insecta|Diptera|Empidinae_fam|নাচের মাছি
Insecta|Diptera|Phorinae_fam|কুঁজো মাছি
Insecta|Diptera|Sarcophaginae_fam|মাংস মাছি
Insecta|Diptera|Anophelinae_fam|ম্যালেরিয়া মশা
Insecta|Diptera|Aedinae_fam|ডেঙ্গু মশা
Insecta|Diptera|Ephydrinae_fam|লবণ মাছি
Insecta|Hemiptera|Pentatominae_fam|গন্ধ পোকা
Insecta|Hemiptera|Coreinae_fam|পা-পাতা পোকা
Insecta|Hemiptera|Reduviinae_fam|অ্যাসাসিন পোকা
Insecta|Hemiptera|Cicadellinae_fam|লিফহপার
Insecta|Hemiptera|Cercopinae_fam|ফেনা পোকা
Insecta|Hemiptera|Aleyrodinae_fam|সাদা মাছি
Insecta|Hemiptera|Aphidinae_fam|আফিড
Insecta|Hemiptera|Coccinae_fam|স্কেল পোকা
Insecta|Hemiptera|Cicadinae_fam|ঝিঁঝি পোকা
Insecta|Hemiptera|Fulgorinae_fam|লণ্ঠন পোকা
Insecta|Hemiptera|Mirinae_fam|প্লান্ট বাগ
Insecta|Hemiptera|Lygaeinae_fam|বীজ পোকা
Insecta|Hemiptera|Tinginae_fam|লেস বাগ
Insecta|Hemiptera|Nepinae_fam|জল বিচ্ছু
Insecta|Hemiptera|Belostomatinae_fam|দৈত্য জল পোকা
Insecta|Hemiptera|Gerrinae_fam|জল মাকড়সা
Insecta|Hemiptera|Naucorinae_fam|ক্রিপিং জল পোকা
Insecta|Hemiptera|Delphacinae_fam|প্লান্টহপার
Insecta|Hemiptera|Notonectinae_fam|উল্টো সাঁতারু পোকা
Insecta|Orthoptera|Acridinae_fam|ঘাসফড়িংগা
Insecta|Orthoptera|Tettigoniinae_fam|ঝিঁঝি
Insecta|Orthoptera|Gryllinae_fam|ঝিঁঝি পোকা
Insecta|Orthoptera|Pyrgomorphinae_fam|রঙিন ফড়িং
Insecta|Orthoptera|Rhaphidophorinae_fam|কেভ ক্রিকেট
Insecta|Orthoptera|Tetriginae_fam|গ্রাউন্ডহপার
Insecta|Orthoptera|Phaneropterinae_fam|বুশ ক্রিকেট
Insecta|Orthoptera|Tridactylinae_fam|পুঁতি ফড়িং
Insecta|Orthoptera|Oxyinae_fam|লতা ফড়িং
Insecta|Odonata|Libellulinae_fam|ড্রাগনফ্লাই
Insecta|Odonata|Aeshninae_fam|দৈত্য ড্রাগনফ্লাই
Insecta|Odonata|Gomphinae_fam|ক্লাবটেইল
Insecta|Odonata|Coenagrioninae_fam|পুকুর ড্যামসেল
Insecta|Odonata|Platycnemidinae_fam|নদী ড্যামসেল
Insecta|Odonata|Lestinae_fam|স্প্রেডউইং
Insecta|Odonata|Macromiinae_fam|দ্রুত ড্রাগনফ্লাই
Insecta|Blattodea|Blattinae_fam|তেলাপোকা
Insecta|Blattodea|Blattellinae_fam|জার্মান তেলাপোকা
Insecta|Blattodea|Blaberinae_fam|দৈত্য তেলাপোকা
Insecta|Blattodea|Termitinae_fam|উইপোকা
Insecta|Blattodea|Rhinotermitinae_fam|ভূমি উইপোকা
Insecta|Blattodea|Kalotermitinae_fam|শুকনো কাঠ উইপোকা
Insecta|Blattodea|Hodotermitinae_fam|শস্য উইপোকা
Insecta|Mantodea|Mantinae_fam|মান্টিস
Insecta|Mantodea|Empusinae_fam|শিং মান্টিস
Insecta|Mantodea|Hymenopodinae_fam|ফুল মান্টিস
Insecta|Neuroptera|Chrysopinae_fam|সবুজ লেসউইং
Insecta|Neuroptera|Myrmeleontinae_fam|অ্যান্টলায়ন
Insecta|Neuroptera|Hemerobiinae_fam|বাদামি লেসউইং
Insecta|Trichoptera|Hydropsychinae_fam|জালি ক্যাডিস
Insecta|Trichoptera|Limnephilinae_fam|উত্তর ক্যাডিস
Insecta|Trichoptera|Leptocerinae_fam|দীর্ঘশৃঙ্গী ক্যাডিস
Insecta|Trichoptera|Philopotaminae_fam|ঝর্ণা ক্যাডিস
Insecta|Ephemeroptera|Ephemerinae_fam|মেফ্লাই
Insecta|Ephemeroptera|Baetinae_fam|নীল ডানা মেফ্লাই
Insecta|Ephemeroptera|Heptageniinae_fam|ফ্ল্যাটহেড মেফ্লাই
Insecta|Plecoptera|Perlinae_fam|স্টোনফ্লাই
Insecta|Plecoptera|Leuctrinae_fam|রোলউইং স্টোনফ্লাই
Insecta|Plecoptera|Chloroperlinae_fam|সবুজ স্টোনফ্লাই
Insecta|Phasmatodea|Phasmatinae_fam|স্টিক ইনসেক্ট
Insecta|Phasmatodea|Heteropteryginae_fam|পাতা পোকা
Arachnida|Araneae|Araneinae_fam|বৃত্ত-জাল মাকড়সা
Arachnida|Araneae|Salticinae_fam|ঝাঁপ দানো মাকড়সা
Arachnida|Araneae|Thomisinae_fam|কাঁকড়া মাকড়সা
Arachnida|Araneae|Lycosinae_fam|নেকড়ে মাকড়সা
Arachnida|Araneae|Theraphosinae_fam|ট্যারান্টুলা
Arachnida|Araneae|Pholcinae_fam|দীর্ঘপা মাকড়সা
Arachnida|Araneae|Oxyopinae_fam|শিকারি মাকড়সা
Arachnida|Araneae|Clubioninae_fam|থলি মাকড়সা
Arachnida|Araneae|Gnaphosinae_fam|মাটি মাকড়সা
Arachnida|Araneae|Uloborinae_fam|বিষহীন জাল মাকড়সা
Arachnida|Araneae|Cteninae_fam|ঘুরে বেড়ানো মাকড়সা
Arachnida|Araneae|Sparassinae_fam|শিকারি বড় মাকড়সা
Arachnida|Araneae|Tetragnathinae_fam|দীর্ঘচোয়াল মাকড়সা
Arachnida|Araneae|Linyphiinae_fam|চাদর-জাল মাকড়সা
Arachnida|Araneae|Barychelinae_fam|ব্রাসফুট ট্যারান্টুলা
Arachnida|Araneae|Eresinae_fam|ভেলভেট মাকড়সা
Arachnida|Araneae|Oonopinae_fam|বামন মাকড়সা
Arachnida|Araneae|Ageleninae_fam|ঘাস জাল মাকড়সা
Arachnida|Araneae|Mimetinae_fam|ডাকাত মাকড়সা
Arachnida|Scorpiones|Scorpioninae_fam|খননকারী বিচ্ছু
Arachnida|Scorpiones|Buthinae_fam|বিষধর বিচ্ছু
Arachnida|Scorpiones|Chactinae_fam|নতুন-পৃথিবী বিচ্ছু
Arachnida|Scorpiones|Iurinae_fam|প্রাচীন বিচ্ছু
Arachnida|Scorpiones|Hormurinae_fam|কাঠ বিচ্ছু
Arachnida|Acari|Ixodinae_fam|শক্ত টিক
Arachnida|Acari|Argasinae_fam|নরম টিক
Arachnida|Acari|Tetranychinae_fam|মাকড়সা-মাইট
Arachnida|Acari|Eriophyinae_fam|গল মাইট
Arachnida|Acari|Sarcoptinae_fam|খোস-মাইট
Arachnida|Acari|Trombiculinae_fam|চিগার মাইট
Arachnida|Acari|Phytoseiinae_fam|শিকারি মাইট
Arachnida|Acari|Oribatinae_fam|মাটি মাইট
Arachnida|Acari|Dermatophagoidinae_fam|ধুলা মাইট
Arachnida|Acari|Varroinae_fam|মৌমাছি মাইট
Arachnida|Acari|Tarsoneminae_fam|ছোট মাইট
Arachnida|Acari|Hydrachninae_fam|জল মাইট
Arachnida|Opiliones|Phalangiinae_fam|সাধারণ হারভেস্টম্যান
Arachnida|Opiliones|Epedaninae_fam|এশীয় হারভেস্টম্যান
Arachnida|Pseudoscorpiones|Cheliferinae_fam|ভুঁয়া বিচ্ছু
Arachnida|Solifugae|Solpuginae_fam|ক্যামেল মাকড়সা
Crustacea|Decapoda|Palaemoninae_fam|মিঠাপানি চিংড়ি
Crustacea|Decapoda|Penaeinae_fam|সামুদ্রিক চিংড়ি
Crustacea|Decapoda|Portuninae_fam|সাঁতারু কাঁকড়া
Crustacea|Decapoda|Grapsinae_fam|তীর কাঁকড়া
Crustacea|Decapoda|Potamonautinae_fam|আফ্রিকান মিঠাপানি কাঁকড়া
Crustacea|Decapoda|Gecarcinucinae_fam|এশীয় মিঠাপানি কাঁকড়া
Crustacea|Decapoda|Astacinae_fam|ক্রাফিশ
Crustacea|Decapoda|Parastacinae_fam|দক্ষিণ ক্রাফিশ
Crustacea|Decapoda|Cambarinae_fam|উত্তর ক্রাফিশ
Crustacea|Decapoda|Pagurinae_fam|হারমিট কাঁকড়া
Crustacea|Decapoda|Diogeninae_fam|ডানহাত হারমিট
Crustacea|Decapoda|Ocypodinae_fam|ফিডলার কাঁকড়া
Crustacea|Decapoda|Scyllarinae_fam|স্লিপার লবস্টার
Crustacea|Decapoda|Nephropinae_fam|লবস্টার
Crustacea|Decapoda|Alpheinae_fam|ঢোল চিংড়ি
Crustacea|Decapoda|Atyinae_fam|ছোট মিঠাপানি চিংড়ি
Crustacea|Decapoda|Hippolytinae_fam|সবজি চিংড়ি
Crustacea|Decapoda|Macrobrachinae_fam|গলদা চিংড়ি
Crustacea|Decapoda|Sesarmatinae_fam|ম্যানগ্রোভ কাঁকড়া
Crustacea|Stomatopoda|Squillinae_fam|ম্যান্টিস শ্রিম্প
Crustacea|Isopoda|Oniscinae_fam|উডলাউস
Crustacea|Isopoda|Cirolaninae_fam|সি আইরোলা
Crustacea|Isopoda|Cymothoinae_fam|জিভ খেকো মাইট
Crustacea|Amphipoda|Gammarinae_fam|গামারাস
Crustacea|Amphipoda|Talitrinae_fam|তীর আম্ফিপড
Crustacea|Amphipoda|Caprellinae_fam|স্কেলিটন শ্রিম্প
Crustacea|Copepoda|Calaninae_fam|কালানাস
Crustacea|Copepoda|Cyclopinae_fam|সাইক্লোপস
Crustacea|Copepoda|Harpacticinae_fam|হারপাকটিকস
Crustacea|Anostraca|Artemiinae_fam|ব্রাইন শ্রিম্প
Crustacea|Anostraca|Streptocephalinae_fam|পরী চিংড়ি
Crustacea|Balanomorpha|Balaninae_fam|বার্নাকল
Crustacea|Branchiopoda|Daphniinae_fam|জল মাছি
Mollusca|Gastropoda_ord|Achatininae_fam|দৈত্য আফ্রিকান শামুক
Mollusca|Gastropoda_ord|Ariophantinae_fam|এশীয় শামুক
Mollusca|Gastropoda_ord|Lymnaeinae_fam|পুকুর শামুক
Mollusca|Gastropoda_ord|Planorbinae_fam|র‍্যামশর্ন শামুক
Mollusca|Gastropoda_ord|Viviparinae_fam|নদী শামুক
Mollusca|Gastropoda_ord|Ampullariinae_fam|আপেল শামুক
Mollusca|Gastropoda_ord|Thiarinae_fam|লাল শামুক
Mollusca|Gastropoda_ord|Camaeninae_fam|স্থল শামুক
Mollusca|Gastropoda_ord|Helicinae_fam|বাগান শামুক
Mollusca|Gastropoda_ord|Veronicellinae_fam|কেঁচো শামুক
Mollusca|Gastropoda_ord|Neritinae_fam|নেরিট শামুক
Mollusca|Gastropoda_ord|Turritellinae_fam|টাওয়ার শামুক
Mollusca|Gastropoda_ord|Cypraeinae_fam|কাউরি শামুক
Mollusca|Gastropoda_ord|Coninae_fam|কোন শামুক
Mollusca|Gastropoda_ord|Volutinae_fam|ভলুট শামুক
Mollusca|Gastropoda_ord|Buccininae_fam|হুইল্ক শামুক
Mollusca|Gastropoda_ord|Muricinae_fam|মুরেক্স শামুক
Mollusca|Gastropoda_ord|Naticinae_fam|মুন শামুক
Mollusca|Gastropoda_ord|Strombinae_fam|কঙ্ক শামুক
Mollusca|Gastropoda_ord|Limacinae_fam|স্লাগ
Mollusca|Gastropoda_ord|Arioninae_fam|গোল স্লাগ
Mollusca|Gastropoda_ord|Succineinae_fam|অ্যাম্বার শামুক
Mollusca|Gastropoda_ord|Melanopsinae_fam|ঝর্ণা শামুক
Mollusca|Gastropoda_ord|Nudibranchia_fam|রঙিন সমুদ্র ফুল
Mollusca|Gastropoda_ord|Haliotinae_fam|অ্যাবালোন
Mollusca|Bivalvia_ord|Unioninae_fam|মিঠাপানি ঝিনুক
Mollusca|Bivalvia_ord|Margaritiferinae_fam|মুক্তা ঝিনুক
Mollusca|Bivalvia_ord|Mytilinae_fam|ঝুঁটি ঝিনুক
Mollusca|Bivalvia_ord|Ostreinae_fam|চিংড়ি ঝিনুক
Mollusca|Bivalvia_ord|Venerinae_fam|ক্ল্যাম ঝিনুক
Mollusca|Bivalvia_ord|Cardiinae_fam|ককল ঝিনুক
Mollusca|Bivalvia_ord|Arcinae_fam|আর্ক ঝিনুক
Mollusca|Bivalvia_ord|Pteriinae_fam|ডানা ঝিনুক
Mollusca|Bivalvia_ord|Tridacninae_fam|দৈত্য ঝিনুক
Mollusca|Bivalvia_ord|Soleninae_fam|রেজর ঝিনুক
Mollusca|Bivalvia_ord|Mactrinae_fam|ম্যাক্ট্রা ঝিনুক
Mollusca|Bivalvia_ord|Corbiculinae_fam|ঝুড়ি ঝিনুক
Mollusca|Bivalvia_ord|Etheriinae_fam|নোঙর ঝিনুক
Mollusca|Bivalvia_ord|Naiadinae_fam|নদী ঝিনুক
Mollusca|Bivalvia_ord|Pinna_fam|পেন ঝিনুক
Mollusca|Cephalopoda_ord|Octopodinae_fam|অক্টোপাস
Mollusca|Cephalopoda_ord|Loligininae_fam|স্কোয়াড
Mollusca|Cephalopoda_ord|Sepiinae_fam|কাটলফিশ
Mollusca|Cephalopoda_ord|Nautilinae_fam|নটিলাস
Mollusca|Cephalopoda_ord|Ommastrephinae_fam|উড়ন্ত স্কোয়াড
Mollusca|Cephalopoda_ord|Architeuthinae_fam|দৈত্য স্কোয়াড
Mollusca|Cephalopoda_ord|Argonautinae_fam|কাগজ নটিলাস
Mollusca|Cephalopoda_ord|Vampyroteuthinae_fam|ভ্যাম্পায়ার স্কোয়াড
Annelida|Haplotaxida|Lumbricinae_fam|কেঁচো
Annelida|Haplotaxida|Megascolecinae_fam|দৈত্য কেঁচো
Annelida|Haplotaxida|Moniligastrinae_fam|এশীয় কেঁচো
Annelida|Haplotaxida|Tubificinae_fam|কাঁদা কেঁচো
Annelida|Hirudinida|Hirudinae_fam|জোঁক
Annelida|Hirudinida|Glossiphoniinae_fam|চ্যাপ্টা জোঁক
Annelida|Polychaeta|Nereinae_fam|রেগাল ওয়ার্ম
Annelida|Polychaeta|Arenicolinae_fam|লাগুন ওয়ার্ম
Annelida|Polychaeta|Terebellinae_fam|নল ওয়ার্ম
Annelida|Polychaeta|Sabellinae_fam|ফ্যান ওয়ার্ম
Annelida|Polychaeta|Eunicinae_fam|বববিট ওয়ার্ম
Annelida|Polychaeta|Aphroditinae_fam|সমুদ্র ইঁদুর
Cnidaria|Actiniaria|Actiniinae_fam|সি অ্যানেমোন
Cnidaria|Actiniaria|Heteractinae_fam|ম্যাগনিফিসেন্ট অ্যানেমোন
Cnidaria|Scleractinia|Acroporinae_fam|শাখা প্রবাল
Cnidaria|Scleractinia|Faviinae_fam|মস্তিষ্ক প্রবাল
Cnidaria|Scleractinia|Poritinae_fam|ফিঙ্গার প্রবাল
Cnidaria|Scleractinia|Mussinae_fam|গোল প্রবাল
Cnidaria|Scleractinia|Dendrophylliinae_fam|কাঠ প্রবাল
Cnidaria|Scleractinia|Turbinariinae_fam|কাঁটাচামচ প্রবাল
Cnidaria|Semaeostomeae|Cyaneinae_fam|সিংহের কেশর জেলি
Cnidaria|Semaeostomeae|Pelagiinae_fam|সমুদ্র-চুল জেলি
Cnidaria|Semaeostomeae|Aureliinae_fam|চাঁদ জেলি
Cnidaria|Siphonophorae|Physalinae_fam|পর্তুগিজ যুদ্ধজাহাজ
Cnidaria|Hydrozoa_ord|Olindinae_fam|হাইড্রোজোয়া
Cnidaria|Hydrozoa_ord|Bougainvilliinae_fam|বুগেনভিলিয়া জেলি
Cnidaria|Hydrozoa_ord|Hydrinae_fam|হাইড্রা
Cnidaria|Hydrozoa_ord|Milleporinae_fam|আগুন প্রবাল
Cnidaria|Alcyonacea|Alcyoniinae_fam|নরম প্রবাল
Cnidaria|Alcyonacea|Gorgoniinae_fam|সমুদ্র পাখা
Cnidaria|Alcyonacea|Pennatulinae_fam|সমুদ্র কলম
Cnidaria|Alcyonacea|Coralliinae_fam|লাল প্রবাল
Cnidaria|Cubozoa_ord|Chironexinae_fam|বাক্স জেলি
Cnidaria|Cubozoa_ord|Carybdeinae_fam|ছোট বাক্স জেলি
Echinodermata|Asteroidea_ord|Asterinae_fam|সাধারণ তারা মাছ
Echinodermata|Asteroidea_ord|Oreasterinae_fam|কুশন তারা
Echinodermata|Asteroidea_ord|Acanthasterinae_fam|কাঁটাযুক্ত তারা
Echinodermata|Asteroidea_ord|Fromiinae_fam|রঙিন তারা
Echinodermata|Echinoidea_ord|Diadematinae_fam|লম্বা কাঁটা আর্চিন
Echinodermata|Echinoidea_ord|Echinometrinae_fam|ছোট কাঁটা আর্চিন
Echinodermata|Echinoidea_ord|Toxopneustinae_fam|ফুল আর্চিন
Echinodermata|Holothuroidea_ord|Holothuriinae_fam|সি কুকুম্বার
Echinodermata|Holothuroidea_ord|Stichopodinae_fam|ডাল সি কুকুম্বার
Echinodermata|Crinoidea_ord|Comasterinae_fam|পালক তারা
Echinodermata|Crinoidea_ord|Tropiometrinae_fam|সমুদ্র লিলি
Echinodermata|Ophiuroidea_ord|Ophiotrichinae_fam|ভঙ্গুর তারা
Echinodermata|Ophiuroidea_ord|Amphiurinae_fam|ছোট ভঙ্গুর তারা
Porifera|Demospongiae_ord|Spongiinae_fam|গোসল স্পঞ্জ
Porifera|Demospongiae_ord|Chondrillinae_fam|গাঢ় স্পঞ্জ
Porifera|Demospongiae_ord|Geodiinae_fam|গিওডিয়া স্পঞ্জ
Porifera|Calcarea_ord|Clathrininae_fam|চুন স্পঞ্জ
Porifera|Hexactinellida_ord|Rossellinae_fam|কাচ স্পঞ্জ
Nematoda|Rhabditida|Rhabditinae_fam|মাটি কৃমি
Nematoda|Rhabditida|Panagrolaiminae_fam|সহনশীল কৃমি
Nematoda|Ascaridida|Ascaridinae_fam|গোল কৃমি
Nematoda|Ascaridida|Toxocarinae_fam|কুকুর গোল কৃমি
Nematoda|Strongylida|Strongylinae_fam|রক্ত কৃমি
Nematoda|Strongylida|Ancylostomatinae_fam|হুক কৃমি
Nematoda|Rhabditida2|Steinernematinae_fam|পতঙ্গমারী কৃমি
Nematoda|Rhabditida2|Heterorhabditinae_fam|শিকারি কৃমি
Nematoda|Tylenchida|Tylenchinae_fam|গাছের গোল কৃমি
Nematoda|Aphelenchoida_fam|Aphelenchoidinae_fam|পাতার গোল কৃমি
Platyhelminthes|Hymenolepidida|Taeniinae_fam|ফিতা কৃমি
Platyhelminthes|Hymenolepidida|Diphyllobothriinae_fam|চওড়া ফিতা কৃমি
Platyhelminthes|Trematoda_ord|Fasciolinae_fam|যকৃৎ ফ্লুক
Platyhelminthes|Trematoda_ord|Schistosomatinae_fam|রক্ত ফ্লুক
Platyhelminthes|Trematoda_ord|Paramphistominae_fam|রুমেন ফ্লুক
Platyhelminthes|Tricladida|Planariinae_fam|প্ল্যানেরিয়া
Platyhelminthes|Tricladida|Geoplaninae_fam|স্থল প্ল্যানেরিয়া
Rotifera|Rotifera_ord|Brachioninae_fam|ব্রাকিওনাস
Rotifera|Rotifera_ord|Lecaninae_fam|লেকেন রোটিফার
Bryozoa|Bryozoa_ord|Bugulinae_fam|ঝোপ ব্রায়োজোয়া
Bryozoa|Bryozoa_ord|Membraniporinae_fam|পর্দা ব্রায়োজোয়া
Bryozoa|Bryozoa_ord|Flustrinae_fam|ফ্লাস্ট্রা
Tardigrada|Tardigrada_ord|Macrobiotinae_fam|স্থল টার্ডিগ্রেড
Tardigrada|Tardigrada_ord|Hypsibiinae_fam|মিঠাপানি টার্ডিগ্রেড
Ascidiacea_ord|Ascidiacea_ord|Styelinae_fam|একক সি স্কয়ার্ট
Ascidiacea_ord|Ascidiacea_ord|Didemninae_fam|কলোনি সি স্কয়ার্ট
Thaliacea_ord|Thaliacea_ord|Salpinae_fam|স্যাল্প
Thaliacea_ord|Thaliacea_ord|Doliolinae_fam|ডোলিওলিয়াম
Branchiostoma_ord|Branchiostoma_ord|Branchiostominae_fam|ল্যান্সলেট
Hemichordata_ord|Hemichordata_ord|Ptychoderinae_fam|অ্যাকর্ন কৃমি
Chaetognatha_ord|Chaetognatha_ord|Sagittinae_fam|অ্যারো কৃমি
Nemertea_ord|Nemertea_ord|Lineinae_fam|রিবন কৃমি
Acoelomorpha_ord|Acoelomorpha_ord|Convolutinae_fam|সবুজ অ্যাসিলোমর্ফ
Ctenophora_ord|Ctenophora_ord|Beroina_fam|কম্ব জেলি
Phoronida_ord|Phoronida_ord|Phoroninae_fam|নাল কৃমি
Sipuncula_ord|Sipuncula_ord|Sipunculinae_fam|চিনাবাদাম কৃমি
Brachiopoda_ord|Brachiopoda_ord|Lingulinae_fam|লিঙ্গুলা
Crustacea|Decapoda|Palaemonidae|চিংড়ি
"""
